package com.isd.wms.service;

import com.isd.wms.dto.allocation.*;
import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.entity.*;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.AllocationsNotFoundException;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.mapper.OperatorSummaryMapper;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TransportUnitRepository;
import com.isd.wms.service.allocation.OperatorExecutionStrategy;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllocationExecutionService {

    private final AllocationRepository allocationRepository;
    private final StockRepository stockRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
    private final ReplenishmentRepository replenishmentRepository;
    private final TransportUnitRepository tuRepository;
    private final SecurityFacade securityFacade;
    private final PickingFlowService pickingFlowService;
    private final OperatorSummaryMapper summaryMapper;

    // Внедряем наши новые стратегии
    private final List<OperatorExecutionStrategy> executionStrategies;

    public Optional<OperatorTaskSummaryResponse> getCurrentSummary() {
        return findCurrentAssignment(securityFacade.getCurrentUsername())
            .map(this::buildSummary)
            .or(() -> findPickedOrderAwaitingCompletion(securityFacade.getCurrentUser())
                .map(this::buildPickingSummary)
                .filter(summary -> summary.currentAllocation() != null));
    }

    @Transactional
    public OperatorTaskSummaryResponse startCurrentTask() {
        CurrentAssignment assignment = findCurrentAssignment(securityFacade.getCurrentUsername())
            .orElseThrow(() -> new InvalidRequestException("No assigned task found"));

        startAllocationExecution(assignment.allocation(), assignment.order());
        return buildSummary(assignment);
    }

    @Transactional
    public void completeCurrentOrder() {
        Order order = findPickedOrderAwaitingCompletion(securityFacade.getCurrentUser())
            .orElseThrow(() -> new InvalidRequestException("No assigned order found"));

        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        boolean allCanceled = orderLines.stream().allMatch(line -> line.getStatus() == Status.CANCELED);
        boolean hasPartialOrCanceled = orderLines.stream().anyMatch(line ->
            line.getStatus() == Status.CANCELED || line.getStatus() == Status.PARTIALLY_COMPLETED || line.getStatus() == Status.SHORTAGE);

        order.setStatus(allCanceled ? OrderStatus.CANCELED : (hasPartialOrCanceled ? OrderStatus.PARTIALLY_COMPLETED : OrderStatus.COMPLETED));
        orderRepository.save(order);

        tuRepository.findByOrder(order).ifPresent(tu -> {
            tu.setOrder(null);
            tuRepository.save(tu);
        });
    }

    public List<AllocationExecutionResponse> getAssignedAllocations() {
        return allocationRepository.findByOperatorAndStatuses(securityFacade.getCurrentUser(), List.of(Status.ASSIGNED, Status.IN_PROGRESS))
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public Long startAllocation() {
        String username = securityFacade.getCurrentUsername();
        Long allocationId = allocationRepository.findOldestAssignedAllocationId(username)
            .orElseThrow(() -> new AllocationsNotFoundException(username));
        Allocation allocation = allocationRepository.findById(allocationId).orElseThrow();
        startAllocationExecution(allocation, null);
        return allocationId;
    }

    @Transactional
    public AllocationExecutionResponse scanSourceLocation(Long allocationId, BarcodeScanRequest request) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        if (!allocation.getStock().getLocation().getBarcode().equals(request.barcode().trim())) {
            throw new InvalidRequestException("Wrong source location barcode");
        }
        allocation.setSourceLocationScanned(true);
        return toResponse(allocationRepository.save(allocation));
    }

    @Transactional
    public AllocationExecutionResponse scanProduct(Long allocationId, BarcodeScanRequest request) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        if (!allocation.isSourceLocationScanned()) throw new InvalidRequestException("Source location must be scanned first");

        Product expectedProduct = allocation.getStock().getProduct().orElseThrow();
        if (!expectedProduct.getBarcode().equalsIgnoreCase(request.barcode().trim())) {
            throw new InvalidRequestException("Wrong product barcode");
        }
        allocation.setProductScanned(true);
        return toResponse(allocationRepository.save(allocation));
    }

    @Transactional
    public AllocationExecutionResponse confirmPickedQuantity(Long allocationId, ConfirmPickedQuantityRequest request) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        if (!allocation.isProductScanned()) throw new InvalidRequestException("Product barcode must be scanned first");

        if (request.pickedQuantity() < 0 || request.pickedQuantity() > allocation.getQuantity() || allocation.getStock().getQuantity() < request.pickedQuantity()) {
            throw new InvalidRequestException("Invalid picked quantity");
        }

        allocation.setPickedQuantity(request.pickedQuantity());
        return toResponse(allocationRepository.save(allocation));
    }

    // ИСПРАВЛЕНИЕ: Метод completeAllocation стал чистым делегатом!
    @Transactional
    public AllocationCompletionResponse completeAllocation(Long allocationId) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        int pickedQuantity = allocation.getPickedQuantity().orElseThrow(() -> new InvalidRequestException("Picked quantity must be confirmed"));
        allocation.setStatus(pickedQuantity == 0 ? Status.CANCELED : (pickedQuantity < allocation.getQuantity() ? Status.PARTIALLY_COMPLETED : Status.COMPLETED));

        Allocation savedAllocation = allocationRepository.save(allocation);

        OperatorExecutionStrategy strategy = executionStrategies.stream()
            .filter(s -> s.supports(allocation.getTask().getTaskType()))
            .findFirst().orElseThrow(() -> new IllegalStateException("No strategy found"));

        return strategy.complete(savedAllocation, pickedQuantity, securityFacade.getCurrentUser());
    }

    @Transactional
    public AllocationCompletionResponse completeAssignedAllocation(Long allocationId) {
        return completeAllocation(allocationId);
    }

    // ИСПРАВЛЕНИЕ: Метод dispatchAllocation тоже стал делегатом!
    @Transactional
    public void dispatchAllocation(Long allocationId, String tuBarcode) {
        Allocation allocation = getAssignedAllocation(allocationId);
        OperatorExecutionStrategy strategy = executionStrategies.stream()
            .filter(s -> s.supports(allocation.getTask().getTaskType()))
            .findFirst().orElseThrow(() -> new IllegalStateException("No strategy found"));

        strategy.dispatch(allocation, tuBarcode);
    }

    // --- Private Helpers ---

    private AllocationExecutionResponse toResponse(Allocation allocation) {
        return new AllocationExecutionResponse(allocation.getId(), allocation.getStatus().name(),
            allocation.isSourceLocationScanned(), allocation.isProductScanned(), allocation.getQuantity(), allocation.getPickedQuantity().orElse(0));
    }

    private Allocation getAssignedAllocationInProgress(Long allocationId) {
        Allocation allocation = getAssignedAllocation(allocationId);
        if (allocation.getStatus() != Status.IN_PROGRESS) throw new InvalidRequestException("Allocation is not in progress");
        return allocation;
    }

    private Allocation getAssignedAllocation(Long allocationId) {
        Allocation allocation = allocationRepository.findById(allocationId).orElseThrow();
        if (allocation.getTask().getOperator().filter(securityFacade.getCurrentUser()::equals).isEmpty()) {
            throw new InvalidRequestException("Allocation is not assigned to current operator");
        }
        return allocation;
    }

    private void startAllocationExecution(Allocation allocation, Order order) {
        if (allocation.getStatus() == Status.IN_PROGRESS) return;
        allocation.setStatus(Status.IN_PROGRESS);

        orderLineRepository.findByTaskId(allocation.getTask().getId()).ifPresent(orderLine -> {
            if (orderLine.getStatus() == Status.ASSIGNED) orderLine.setStatus(Status.IN_PROGRESS);
            Order currentOrder = order != null ? order : orderLine.getOrder();
            if (currentOrder.getStatus() == OrderStatus.ASSIGNED) {
                currentOrder.setStatus(OrderStatus.IN_PROGRESS);
                orderRepository.save(currentOrder);
            }
        });

        replenishmentRepository.findByTaskId(allocation.getTask().getId()).ifPresent(replenishment -> {
            if (replenishment.getStatus() == Status.ASSIGNED) replenishment.setStatus(Status.IN_PROGRESS);
        });
    }

    private Optional<CurrentAssignment> findCurrentAssignment(String username) {
        List<Allocation> activeAllocations = allocationRepository.findByOperatorUsernameAndStatuses(username, List.of(Status.CREATED, Status.ASSIGNED, Status.IN_PROGRESS));
        if (activeAllocations.isEmpty()) return Optional.empty();

        Allocation first = activeAllocations.getFirst();
        if (first.getTask().getTaskType() == TaskType.PICKING_ORDER) {
            Order order = orderLineRepository.findByTaskId(first.getTask().getId()).map(OrderLine::getOrder).orElseThrow();
            List<Allocation> ord = pickingFlowService.orderAllocationsBySourceLocation(allocationRepository.findAllByOrder(order));
            return pickingFlowService.findCurrentExecutableAllocation(ord).map(a -> new CurrentAssignment(a, TaskType.PICKING_ORDER, order));
        }

        List<Allocation> replAllocations = allocationRepository.findAllByTaskId(first.getTask().getId());
        List<Allocation> orderedReplAllocations = pickingFlowService.orderAllocationsBySourceLocation(replAllocations);
        return pickingFlowService.findCurrentExecutableAllocation(orderedReplAllocations).map(a -> new CurrentAssignment(a, TaskType.REPLENISHMENT, null));
    }

    private OperatorTaskSummaryResponse buildSummary(CurrentAssignment assignment) {
        if (assignment.taskType() == TaskType.PICKING_ORDER) return buildPickingSummary(assignment.order());

        Replenishment replenishment = replenishmentRepository.findByTaskId(assignment.allocation().getTask().getId()).orElseThrow();
        List<Allocation> taskAllocations = allocationRepository.findAllByTaskId(assignment.allocation().getTask().getId()).stream()
            .sorted(Comparator.comparing(Allocation::getCreatedAt).thenComparing(Allocation::getId)).toList();

        return summaryMapper.toReplenishmentSummary(assignment.allocation().getTask(), replenishment, taskAllocations, assignment.allocation(), tuRepository.existsByReplenishment(replenishment));
    }

    private OperatorTaskSummaryResponse buildPickingSummary(Order order) {
        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        List<Allocation> orderedAllocations = pickingFlowService.orderAllocationsBySourceLocation(allocationRepository.findAllByOrder(order));
        Allocation currentAllocation = pickingFlowService.findCurrentExecutableAllocation(orderedAllocations).orElse(null);
        return summaryMapper.toPickingSummary(order, orderLines, orderedAllocations, currentAllocation, tuRepository.existsByOrder(order));
    }

    private Optional<Order> findPickedOrderAwaitingCompletion(User operator) {
        return orderRepository.findOldestPickedOrderAssignedToOperator(operator.getId());
    }

    private record CurrentAssignment(Allocation allocation, TaskType taskType, Order order) {}
}
