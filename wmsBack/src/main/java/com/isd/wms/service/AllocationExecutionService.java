package com.isd.wms.service;

import com.isd.wms.dto.operator.OperatorOrderLineSummaryResponse;
import com.isd.wms.dto.operator.OperatorAllocationSummaryResponse;
import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.dto.allocation.*;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.AllocationsNotFoundException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
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

    private final AllocationRepository  allocationRepository ;
    private final StockRepository stockRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
    private final ReplenishmentRepository replenishmentRepository;
    private final InventoryService inventoryService;
    private final SecurityFacade securityFacade;
    private final WorkflowService workflowService;
    private final PickingFlowService pickingFlowService;

    public Optional<OperatorTaskSummaryResponse> getCurrentSummary() {
        Optional<CurrentAssignment> assignment = findCurrentAssignment(securityFacade.getCurrentUsername());
        if (assignment.isPresent()) {
            return Optional.of(assignment.get().taskType() == TaskType.PICKING_ORDER
                ? toPickingSummary(assignment.get().order())
                : toReplenishmentSummary(assignment.get().allocation()));
        }

        return findPickedOrderAwaitingCompletion(securityFacade.getCurrentUser())
            .map(this::toPickingSummary);
    }

    @Transactional
    public OperatorTaskSummaryResponse startCurrentTask() {
        CurrentAssignment assignment = findCurrentAssignment(securityFacade.getCurrentUsername())
            .orElseThrow(() -> new InvalidRequestException("No assigned task found for current operator"));

        if (assignment.taskType() == TaskType.PICKING_ORDER) {
            startAllocationExecution(assignment.allocation(), assignment.order());
            return toPickingSummary(assignment.order());
        }

        startAllocationExecution(assignment.allocation(), null);
        return toReplenishmentSummary(assignment.allocation());
    }

    @Transactional
    public void completeCurrentOrder() {
        Order order = findPickedOrderAwaitingCompletion(securityFacade.getCurrentUser())
            .orElseThrow(() -> new InvalidRequestException("No assigned order found for current operator"));

        if (order.getStatus() != OrderStatus.PICKED) {
            throw new InvalidRequestException("Order is not ready for final completion");
        }

        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        if (orderLines.isEmpty()) {
            throw new InvalidRequestException("Order has no lines to complete");
        }

        boolean allLinesCompleted = orderLines.stream().allMatch(line -> line.getStatus() == Status.COMPLETED);
        if (!allLinesCompleted) {
            throw new InvalidRequestException("All order lines must be completed before final confirmation");
        }

        List<Allocation> allocations = allocationRepository.findAllByOrder(order);
        boolean allAllocationsCompleted = allocations.stream().allMatch(allocation -> allocation.getStatus() == Status.COMPLETED);
        if (!allAllocationsCompleted) {
            throw new InvalidRequestException("All allocations must be completed before final confirmation");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    public List<AllocationExecutionResponse> getAssignedAllocations() {
        User operator = securityFacade.getCurrentUser();
        return allocationRepository.findByOperatorAndStatuses(
                operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public Long startAllocation() {
        String currentUsername = securityFacade.getCurrentUsername();
        Long allocationId = allocationRepository.findOldestAssignedAllocationId(currentUsername)
            .orElseThrow(() -> new AllocationsNotFoundException(currentUsername));

        Allocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new InvalidRequestException("Allocation not found"));

        orderLineRepository.findByTaskId(allocation.getTask().getId()).ifPresent(orderLine -> {
            if (orderLine.getStatus() == Status.ASSIGNED) {
                orderLine.setStatus(Status.IN_PROGRESS);
            }

            if (orderLine.getOrder().getStatus() == OrderStatus.ASSIGNED) {
                orderLine.getOrder().setStatus(OrderStatus.IN_PROGRESS);
                orderRepository.save(orderLine.getOrder());
            }
        });

        return allocationId;
    }

    @Transactional
    public AllocationExecutionResponse scanSourceLocation(Long allocationId, BarcodeScanRequest request) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        String barcode = request.barcode().trim();
        String expectedBarcode = allocation.getStock().getLocation().getBarcode();

        if (!expectedBarcode.equals(barcode)) {
            log.warn("Wrong barcode scanned for allocation {}", allocation);
            throw new InvalidRequestException("Wrong source location barcode");
        }

        allocation.setSourceLocationScanned(true);
        log.info("Source location scanned successfully for allocation {}", allocationId);
        return toResponse(allocationRepository.save(allocation));
    }

    @Transactional
    public AllocationExecutionResponse scanProduct(Long allocationId, BarcodeScanRequest request) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        if (!allocation.isSourceLocationScanned()) {
            throw new InvalidRequestException("Source location must be scanned first");
        }

        String barcode = request.barcode().trim();
        Stock expectedStock = allocation.getStock();
        Product expectedProduct = expectedStock.getProduct()
            .filter(product -> product.getBarcode() != null && product.getBarcode().equalsIgnoreCase(barcode))
            .orElse(null);
        if (expectedProduct == null) {
            log.warn("Wrong barcode scanned for allocation {}", allocationId);
            throw new InvalidRequestException("Wrong product barcode");
        }

        stockRepository.findByProductIdAndLocationId(
                expectedProduct.getId(),
                expectedStock.getLocation().getId())
            .orElseThrow(() -> new StockNotFoundException(expectedStock.getId()));

        allocation.setProductScanned(true);
        log.info("Product barcode scanned successfully for allocation {}", allocationId);
        return toResponse(allocationRepository.save(allocation));
    }

    @Transactional
    public AllocationExecutionResponse confirmPickedQuantity(Long allocationId, ConfirmPickedQuantityRequest request) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        if (!allocation.isProductScanned()) {
            throw new InvalidRequestException("Product barcode must be scanned first");
        }

        Integer pickedQuantity = request.pickedQuantity();
        validatePickedQuantityForAllocation(allocation, pickedQuantity);

        allocation.setPickedQuantity(pickedQuantity);
        log.info("Picked quantity {} confirmed for allocation {}", pickedQuantity, allocationId);
        return toResponse(allocationRepository.save(allocation));
    }

    @Transactional
    public AllocationCompletionResponse completeAllocation(Long allocationId) {
        Allocation allocation = getAssignedAllocationInProgress(allocationId);
        User operator = securityFacade.getCurrentUser();

        if (!allocation.isSourceLocationScanned()) {
            throw new InvalidRequestException("Source location must be scanned first");
        }
        if (!allocation.isProductScanned()) {
            throw new InvalidRequestException("Product barcode must be scanned first");
        }
        if (allocation.getPickedQuantity() == null) {
            throw new InvalidRequestException("Picked quantity must be confirmed before completion");
        }

        validatePickedQuantityForAllocation(allocation, allocation.getPickedQuantity());

        allocation.setStatus(Status.COMPLETED);
        Allocation savedAllocation = allocationRepository.save(allocation);

        AllocationCompletionResult result = workflowService.executeAllocationCompletion(savedAllocation);
        inventoryService.recordPickingHistory(savedAllocation.getStock(), savedAllocation.getPickedQuantity(), operator);
        autoAdvanceGroupedPickingFlow(savedAllocation);

        log.info("Allocation {} completed by operator {}", allocationId, operator.getUsername());
        return new AllocationCompletionResponse(result);
    }

    @Transactional
    public AllocationCompletionResponse completeAssignedAllocation(Long allocationId) {
        return completeAllocation(allocationId);
    }

    private Optional<Order> findPickedOrderAwaitingCompletion(User operator) {
        return orderRepository.findOldestPickedOrderAssignedToOperator(operator.getId());
    }

    private Optional<CurrentAssignment> findCurrentAssignment(String username) {
        List<Allocation> activeAllocations = allocationRepository.findByOperatorUsernameAndStatuses(
            username,
            List.of(Status.ASSIGNED, Status.IN_PROGRESS)
        );
        if (activeAllocations.isEmpty()) {
            return Optional.empty();
        }

        Allocation earliestAssignedAllocation = activeAllocations.getFirst();
        if (earliestAssignedAllocation.getTask().getTaskType() == TaskType.PICKING_ORDER) {
            Order order = orderLineRepository.findByTaskId(earliestAssignedAllocation.getTask().getId())
                .map(OrderLine::getOrder)
                .orElseThrow(() -> new InvalidRequestException("Order not found for picking task"));

            List<Allocation> orderedAllocations = pickingFlowService.orderAllocationsBySourceLocation(allocationRepository.findAllByOrder(order));
            Optional<Allocation> currentAllocation = pickingFlowService.findCurrentExecutableAllocation(orderedAllocations);
            if (currentAllocation.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new CurrentAssignment(currentAllocation.get(), TaskType.PICKING_ORDER, order));
        }

        return Optional.of(new CurrentAssignment(earliestAssignedAllocation, TaskType.REPLENISHMENT, null));
    }

    private void startAllocationExecution(Allocation allocation, Order order) {
        if (allocation.getStatus() == Status.IN_PROGRESS) {
            return;
        }
        if (allocation.getStatus() != Status.ASSIGNED) {
            throw new InvalidRequestException("Allocation is not available to start");
        }

        allocation.setStatus(Status.IN_PROGRESS);

        orderLineRepository.findByTaskId(allocation.getTask().getId()).ifPresent(orderLine -> {
            if (orderLine.getStatus() == Status.ASSIGNED) {
                orderLine.setStatus(Status.IN_PROGRESS);
            }

            Order currentOrder = order != null ? order : orderLine.getOrder();
            if (currentOrder.getStatus() == OrderStatus.ASSIGNED) {
                currentOrder.setStatus(OrderStatus.IN_PROGRESS);
                orderRepository.save(currentOrder);
            }
        });

        replenishmentRepository.findByTaskId(allocation.getTask().getId()).ifPresent(replenishment -> {
            if (replenishment.getStatus() == Status.ASSIGNED) {
                replenishment.setStatus(Status.IN_PROGRESS);
            }
        });
    }

    private OperatorTaskSummaryResponse toPickingSummary(Order order) {
        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        List<Allocation> orderedAllocations = pickingFlowService.orderAllocationsBySourceLocation(allocationRepository.findAllByOrder(order));

        Allocation currentAllocation = pickingFlowService.findCurrentExecutableAllocation(orderedAllocations).orElse(null);

        List<OperatorOrderLineSummaryResponse> lineSummaries = orderLines.stream()
            .sorted(Comparator.comparing(OrderLine::getCreatedAt).thenComparing(OrderLine::getId))
            .map(orderLine -> toLineSummary(order, orderLine, orderedAllocations))
            .toList();

        long completedAllocationCount = orderedAllocations.stream()
            .filter(allocation -> allocation.getStatus() == Status.COMPLETED)
            .count();

        boolean readyForCompletion = order.getStatus() == OrderStatus.PICKED
            && orderedAllocations.stream().allMatch(allocation -> allocation.getStatus() == Status.COMPLETED);


        return new OperatorTaskSummaryResponse(
            currentAllocation != null ? currentAllocation.getTask().getId() : orderLines.stream().findFirst().map(
                AllocationExecutionService::getTaskId).orElse(null),
            order.getId(),
            order.getLogicId(),
            order.getStatus(),
            TaskType.PICKING_ORDER.name(),
            order.getDestinationLocation().getBarcode(),
            orderedAllocations.size(),
            Math.toIntExact(completedAllocationCount),
            readyForCompletion,
            currentAllocation != null ? toAllocationSummary(currentAllocation, order) : null,
            lineSummaries,
            orderedAllocations.stream().map(allocation -> toAllocationSummary(allocation, order)).toList()
        );
    }

    private static Long getTaskId(OrderLine orderLine) {
        return orderLine.getTask().orElseThrow(() -> new InvalidRequestException(
            "No task found for order line " + orderLine.getId())).getId();
    }

    private OperatorTaskSummaryResponse toReplenishmentSummary(Allocation currentAllocation) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(currentAllocation.getTask().getId())
            .orElseThrow(() -> new InvalidRequestException("Replenishment task not found"));

        List<Allocation> taskAllocations = allocationRepository.findAllByTaskId(currentAllocation.getTask().getId()).stream()
            .sorted(Comparator.comparing(Allocation::getCreatedAt).thenComparing(Allocation::getId))
            .toList();

        long completedAllocationCount = taskAllocations.stream()
            .filter(allocation -> allocation.getStatus() == Status.COMPLETED)
            .count();

        return new OperatorTaskSummaryResponse(
            currentAllocation.getTask().getId(),
            null,
            null,
            null,
            currentAllocation.getTask().getTaskType().name(),
            replenishment.getDestinationLocation().getBarcode(),
            taskAllocations.size(),
            Math.toIntExact(completedAllocationCount),
            false,
            toAllocationSummary(currentAllocation, replenishment.getDestinationLocation().getBarcode()),
            List.of(),
            taskAllocations.stream()
                .map(allocation -> toAllocationSummary(allocation, replenishment.getDestinationLocation().getBarcode()))
                .toList()
        );
    }

    private OperatorOrderLineSummaryResponse toLineSummary(Order order, OrderLine orderLine, List<Allocation> orderedAllocations) {
        List<Allocation> lineAllocations = orderedAllocations.stream()
            .filter(allocation -> allocation.getTask().getId().equals(getTaskId(orderLine)))
            .toList();

        int pickedQuantity = lineAllocations.stream()
            .filter(allocation -> allocation.getStatus() == Status.COMPLETED)
            .mapToInt(allocation -> allocation.getPickedQuantity() != null ? allocation.getPickedQuantity() : allocation.getQuantity())
            .sum();

        List<String> sourceLocationBarcodes = lineAllocations.stream()
            .map(allocation -> allocation.getStock().getLocation().getBarcode())
            .distinct()
            .toList();

        return new OperatorOrderLineSummaryResponse(
            getTaskId(orderLine),
            orderLine.getId(),
            orderLine.getProduct().getId(),
            orderLine.getProduct().getName(),
            orderLine.getProduct().getBarcode(),
            orderLine.getRequestedQuantity(),
            pickedQuantity,
            sourceLocationBarcodes,
            order.getDestinationLocation().getBarcode(),
            orderLine.getStatus()
        );
    }

    private OperatorAllocationSummaryResponse toAllocationSummary(Allocation allocation, Order order) {
        return toAllocationSummary(allocation, order != null ? order.getDestinationLocation().getBarcode() : allocation.getStock().getLocation().getBarcode());
    }

    private OperatorAllocationSummaryResponse toAllocationSummary(Allocation allocation, String destinationLocationBarcode) {
        Stock stock = allocation.getStock();
        return new OperatorAllocationSummaryResponse(
            allocation.getId(),
            allocation.getTask().getId(),
            orderLineRepository.findByTaskId(allocation.getTask().getId()).map(OrderLine::getId).orElse(null),
            stock.getProduct().map(product -> product.getId()).orElse(null),
            stock.getProduct().map(product -> product.getName()).orElse(null),
            stock.getProduct().map(product -> product.getBarcode()).orElse(null),
            stock.getLocation().getBarcode(),
            destinationLocationBarcode,
            allocation.getQuantity(),
            allocation.getPickedQuantity(),
            allocation.getStatus(),
            allocation.isSourceLocationScanned(),
            allocation.isProductScanned()
        );
    }

    private record CurrentAssignment(Allocation allocation, TaskType taskType, Order order) {
    }

    private Allocation getAssignedAllocationInProgress(Long allocationId) {
        Allocation allocation = getAssignedAllocation(allocationId);
        if (allocation.getStatus() == Status.COMPLETED) {
            throw new InvalidRequestException("Allocation is already completed");
        }
        if (allocation.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Allocation is cancelled");
        }
        if (allocation.getStatus() != Status.IN_PROGRESS) {
            throw new InvalidRequestException("Allocation is not in progress");
        }
        return allocation;
    }

    private Allocation getAssignedAllocation(Long allocationId) {
        Allocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new InvalidRequestException("Allocation not found"));
        User operator = securityFacade.getCurrentUser();
        if (allocation.getTask().getOperator().filter(operator::equals).isEmpty()) {
            throw new InvalidRequestException("Allocation is not assigned to current operator");
        }
        return allocation;
    }

    private void validatePickedQuantityForAllocation(Allocation allocation, Integer pickedQuantity) {
        if (pickedQuantity == null || pickedQuantity < 1) {
            throw new InvalidRequestException("Picked quantity must be greater than 0");
        }
        if (pickedQuantity > allocation.getQuantity()) {
            throw new InvalidRequestException("Picked quantity cannot exceed required quantity");
        }
        if (allocation.getTask().getTaskType() == TaskType.PICKING_ORDER && !pickedQuantity.equals(allocation.getQuantity())) {
            throw new InvalidRequestException("Picked quantity must match required quantity for picking tasks");
        }
        if (allocation.getStock().getQuantity() < pickedQuantity) {
            throw new InvalidRequestException("Not enough stock available");
        }
    }

    private void autoAdvanceGroupedPickingFlow(Allocation completedAllocation) {
        if (completedAllocation.getTask().getTaskType() != TaskType.PICKING_ORDER) {
            return;
        }

        orderLineRepository.findByTaskId(completedAllocation.getTask().getId()).ifPresent(orderLine -> {
            List<Allocation> orderAllocations = allocationRepository.findAllByOrder(orderLine.getOrder());
            pickingFlowService.findNextExecutableAllocationAfter(orderAllocations, completedAllocation)
                .ifPresent(nextAllocation -> {
                    if (nextAllocation.getStatus() == Status.ASSIGNED) {
                        nextAllocation.setStatus(Status.IN_PROGRESS);
                    }

                    boolean sameSourceLocation = nextAllocation.getStock().getLocation().getId()
                        .equals(completedAllocation.getStock().getLocation().getId());

                    if (sameSourceLocation) {
                        nextAllocation.setSourceLocationScanned(true);
                    }

                    allocationRepository.save(nextAllocation);
                });
        });
    }

    private AllocationExecutionResponse toResponse(Allocation allocation) {
        return new AllocationExecutionResponse(
            allocation.getId(),
            allocation.getStatus().name(),
            allocation.isSourceLocationScanned(),
            allocation.isProductScanned(),
            allocation.getQuantity(),
            allocation.getPickedQuantity()
        );
    }
}
