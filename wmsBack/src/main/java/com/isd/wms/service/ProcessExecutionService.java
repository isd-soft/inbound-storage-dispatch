package com.isd.wms.service;

import com.isd.wms.dto.operator.OperatorOrderLineSummaryResponse;
import com.isd.wms.dto.operator.OperatorProcessSummaryResponse;
import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.dto.process.*;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProcessesNotFoundException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ProcessRepository;
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
public class ProcessExecutionService {

    private final ProcessRepository processRepository;
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
                : toReplenishmentSummary(assignment.get().process()));
        }

        return findPickedOrderAwaitingCompletion(securityFacade.getCurrentUser())
            .map(this::toPickingSummary);
    }

    @Transactional
    public OperatorTaskSummaryResponse startCurrentTask() {
        CurrentAssignment assignment = findCurrentAssignment(securityFacade.getCurrentUsername())
            .orElseThrow(() -> new InvalidRequestException("No assigned task found for current operator"));

        if (assignment.taskType() == TaskType.PICKING_ORDER) {
            startProcessExecution(assignment.process(), assignment.order());
            return toPickingSummary(assignment.order());
        }

        startProcessExecution(assignment.process(), null);
        return toReplenishmentSummary(assignment.process());
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

        List<Process> processes = processRepository.findAllByOrder(order);
        boolean allProcessesCompleted = processes.stream().allMatch(process -> process.getStatus() == Status.COMPLETED);
        if (!allProcessesCompleted) {
            throw new InvalidRequestException("All processes must be completed before final confirmation");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    public List<ProcessExecutionResponse> getAssignedProcesses() {
        User operator = securityFacade.getCurrentUser();
        return processRepository.findByOperatorAndStatuses(
                operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public Long startProcess() {
        String currentUsername = securityFacade.getCurrentUsername();
        Long processId = processRepository.findOldestAssignedProcessId(currentUsername)
            .orElseThrow(() -> new ProcessesNotFoundException(currentUsername));

        Process process = processRepository.findById(processId)
            .orElseThrow(() -> new InvalidRequestException("Process not found"));

        orderLineRepository.findByTaskId(process.getTask().getId()).ifPresent(orderLine -> {
            if (orderLine.getStatus() == Status.ASSIGNED) {
                orderLine.setStatus(Status.IN_PROGRESS);
            }

            if (orderLine.getOrder().getStatus() == OrderStatus.ASSIGNED) {
                orderLine.getOrder().setStatus(OrderStatus.IN_PROGRESS);
                orderRepository.save(orderLine.getOrder());
            }
        });

        return processId;
    }

    @Transactional
    public ProcessExecutionResponse scanSourceLocation(Long processId, BarcodeScanRequest request) {
        Process process = getAssignedProcessInProgress(processId);
        String barcode = request.barcode().trim();
        String expectedBarcode = process.getStock().getLocation().getBarcode();

        if (!expectedBarcode.equals(barcode)) {
            log.warn("Wrong barcode scanned for process {}", processId);
            throw new InvalidRequestException("Wrong source location barcode");
        }

        process.setSourceLocationScanned(true);
        log.info("Source location scanned successfully for process {}", processId);
        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessExecutionResponse scanProduct(Long processId, BarcodeScanRequest request) {
        Process process = getAssignedProcessInProgress(processId);
        if (!process.isSourceLocationScanned()) {
            throw new InvalidRequestException("Source location must be scanned first");
        }

        String barcode = request.barcode().trim();
        Stock expectedStock = process.getStock();
        Product expectedProduct = expectedStock.getProduct()
            .filter(product -> product.getBarcode() != null && product.getBarcode().equalsIgnoreCase(barcode))
            .orElse(null);
        if (expectedProduct == null) {
            log.warn("Wrong barcode scanned for process {}", processId);
            throw new InvalidRequestException("Wrong product barcode");
        }

        stockRepository.findByProductIdAndLocationId(
                expectedProduct.getId(),
                expectedStock.getLocation().getId())
            .orElseThrow(() -> new StockNotFoundException(expectedStock.getId()));

        process.setProductScanned(true);
        log.info("Product barcode scanned successfully for process {}", processId);
        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessExecutionResponse confirmPickedQuantity(Long processId, ConfirmPickedQuantityRequest request) {
        Process process = getAssignedProcessInProgress(processId);
        if (!process.isProductScanned()) {
            throw new InvalidRequestException("Product barcode must be scanned first");
        }

        Integer pickedQuantity = request.pickedQuantity();
        validatePickedQuantityForProcess(process, pickedQuantity);

        process.setPickedQuantity(pickedQuantity);
        log.info("Picked quantity {} confirmed for process {}", pickedQuantity, processId);
        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessCompletionResponse completeProcess(Long processId) {
        Process process = getAssignedProcessInProgress(processId);
        User operator = securityFacade.getCurrentUser();

        if (!process.isSourceLocationScanned()) {
            throw new InvalidRequestException("Source location must be scanned first");
        }
        if (!process.isProductScanned()) {
            throw new InvalidRequestException("Product barcode must be scanned first");
        }
        if (process.getPickedQuantity() == null) {
            throw new InvalidRequestException("Picked quantity must be confirmed before completion");
        }

        validatePickedQuantityForProcess(process, process.getPickedQuantity());

        process.setStatus(Status.COMPLETED);
        Process savedProcess = processRepository.save(process);

        ProcessCompletionResult result = workflowService.executeProcessCompletion(savedProcess);
        inventoryService.recordPickingHistory(savedProcess.getStock(), savedProcess.getPickedQuantity(), operator);
        autoAdvanceGroupedPickingFlow(savedProcess);

        log.info("Process {} completed by operator {}", processId, operator.getUsername());
        return new ProcessCompletionResponse(result);
    }

    @Transactional
    public ProcessCompletionResponse completeAssignedProcess(Long processId) {
        return completeProcess(processId);
    }

    private Optional<Order> findPickedOrderAwaitingCompletion(User operator) {
        return orderRepository.findOldestPickedOrderAssignedToOperator(operator.getId());
    }

    private Optional<CurrentAssignment> findCurrentAssignment(String username) {
        List<Process> activeProcesses = processRepository.findByOperatorUsernameAndStatuses(
            username,
            List.of(Status.ASSIGNED, Status.IN_PROGRESS)
        );
        if (activeProcesses.isEmpty()) {
            return Optional.empty();
        }

        Process earliestAssignedProcess = activeProcesses.getFirst();
        if (earliestAssignedProcess.getTask().getTaskType() == TaskType.PICKING_ORDER) {
            Order order = orderLineRepository.findByTaskId(earliestAssignedProcess.getTask().getId())
                .map(OrderLine::getOrder)
                .orElseThrow(() -> new InvalidRequestException("Order not found for picking task"));

            List<Process> orderedProcesses = pickingFlowService.orderProcessesBySourceLocation(processRepository.findAllByOrder(order));
            Optional<Process> currentProcess = pickingFlowService.findCurrentExecutableProcess(orderedProcesses);
            if (currentProcess.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new CurrentAssignment(currentProcess.get(), TaskType.PICKING_ORDER, order));
        }

        return Optional.of(new CurrentAssignment(earliestAssignedProcess, TaskType.REPLENISHMENT, null));
    }

    private void startProcessExecution(Process process, Order order) {
        if (process.getStatus() == Status.IN_PROGRESS) {
            return;
        }
        if (process.getStatus() != Status.ASSIGNED) {
            throw new InvalidRequestException("Process is not available to start");
        }

        process.setStatus(Status.IN_PROGRESS);

        orderLineRepository.findByTaskId(process.getTask().getId()).ifPresent(orderLine -> {
            if (orderLine.getStatus() == Status.ASSIGNED) {
                orderLine.setStatus(Status.IN_PROGRESS);
            }

            Order currentOrder = order != null ? order : orderLine.getOrder();
            if (currentOrder.getStatus() == OrderStatus.ASSIGNED) {
                currentOrder.setStatus(OrderStatus.IN_PROGRESS);
                orderRepository.save(currentOrder);
            }
        });

        replenishmentRepository.findByTaskId(process.getTask().getId()).ifPresent(replenishment -> {
            if (replenishment.getStatus() == Status.ASSIGNED) {
                replenishment.setStatus(Status.IN_PROGRESS);
            }
        });
    }

    private OperatorTaskSummaryResponse toPickingSummary(Order order) {
        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        List<Process> orderedProcesses = pickingFlowService.orderProcessesBySourceLocation(processRepository.findAllByOrder(order));

        Process currentProcess = pickingFlowService.findCurrentExecutableProcess(orderedProcesses).orElse(null);

        List<OperatorOrderLineSummaryResponse> lineSummaries = orderLines.stream()
            .sorted(Comparator.comparing(OrderLine::getCreatedAt).thenComparing(OrderLine::getId))
            .map(orderLine -> toLineSummary(order, orderLine, orderedProcesses))
            .toList();

        long completedProcessCount = orderedProcesses.stream()
            .filter(process -> process.getStatus() == Status.COMPLETED)
            .count();

        boolean readyForCompletion = order.getStatus() == OrderStatus.PICKED
            && orderedProcesses.stream().allMatch(process -> process.getStatus() == Status.COMPLETED);

        return new OperatorTaskSummaryResponse(
            currentProcess != null ? currentProcess.getTask().getId() : orderLines.stream().findFirst().map(orderLine -> orderLine.getTask().getId()).orElse(null),
            order.getId(),
            order.getLogicId(),
            order.getStatus(),
            TaskType.PICKING_ORDER.name(),
            order.getDestinationLocation().getBarcode(),
            orderedProcesses.size(),
            Math.toIntExact(completedProcessCount),
            readyForCompletion,
            currentProcess != null ? toProcessSummary(currentProcess, order) : null,
            lineSummaries,
            orderedProcesses.stream().map(process -> toProcessSummary(process, order)).toList()
        );
    }

    private OperatorTaskSummaryResponse toReplenishmentSummary(Process currentProcess) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(currentProcess.getTask().getId())
            .orElseThrow(() -> new InvalidRequestException("Replenishment task not found"));

        List<Process> taskProcesses = processRepository.findAllByTaskId(currentProcess.getTask().getId()).stream()
            .sorted(Comparator.comparing(Process::getCreatedAt).thenComparing(Process::getId))
            .toList();

        long completedProcessCount = taskProcesses.stream()
            .filter(process -> process.getStatus() == Status.COMPLETED)
            .count();

        return new OperatorTaskSummaryResponse(
            currentProcess.getTask().getId(),
            null,
            null,
            null,
            currentProcess.getTask().getTaskType().name(),
            replenishment.getDestinationLocation().getBarcode(),
            taskProcesses.size(),
            Math.toIntExact(completedProcessCount),
            false,
            toProcessSummary(currentProcess, replenishment.getDestinationLocation().getBarcode()),
            List.of(),
            taskProcesses.stream()
                .map(process -> toProcessSummary(process, replenishment.getDestinationLocation().getBarcode()))
                .toList()
        );
    }

    private OperatorOrderLineSummaryResponse toLineSummary(Order order, OrderLine orderLine, List<Process> orderedProcesses) {
        List<Process> lineProcesses = orderedProcesses.stream()
            .filter(process -> process.getTask().getId().equals(orderLine.getTask().getId()))
            .toList();

        int pickedQuantity = lineProcesses.stream()
            .filter(process -> process.getStatus() == Status.COMPLETED)
            .mapToInt(process -> process.getPickedQuantity() != null ? process.getPickedQuantity() : process.getQuantity())
            .sum();

        List<String> sourceLocationBarcodes = lineProcesses.stream()
            .map(process -> process.getStock().getLocation().getBarcode())
            .distinct()
            .toList();

        return new OperatorOrderLineSummaryResponse(
            orderLine.getTask().getId(),
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

    private OperatorProcessSummaryResponse toProcessSummary(Process process, Order order) {
        return toProcessSummary(process, order != null ? order.getDestinationLocation().getBarcode() : process.getStock().getLocation().getBarcode());
    }

    private OperatorProcessSummaryResponse toProcessSummary(Process process, String destinationLocationBarcode) {
        Stock stock = process.getStock();
        return new OperatorProcessSummaryResponse(
            process.getId(),
            process.getTask().getId(),
            orderLineRepository.findByTaskId(process.getTask().getId()).map(OrderLine::getId).orElse(null),
            stock.getProduct().map(product -> product.getId()).orElse(null),
            stock.getProduct().map(product -> product.getName()).orElse(null),
            stock.getProduct().map(product -> product.getBarcode()).orElse(null),
            stock.getLocation().getBarcode(),
            destinationLocationBarcode,
            process.getQuantity(),
            process.getPickedQuantity(),
            process.getStatus(),
            process.isSourceLocationScanned(),
            process.isProductScanned()
        );
    }

    private record CurrentAssignment(Process process, TaskType taskType, Order order) {
    }

    private Process getAssignedProcessInProgress(Long processId) {
        Process process = getAssignedProcess(processId);
        if (process.getStatus() == Status.COMPLETED) {
            throw new InvalidRequestException("Process is already completed");
        }
        if (process.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Process is cancelled");
        }
        if (process.getStatus() != Status.IN_PROGRESS) {
            throw new InvalidRequestException("Process is not in progress");
        }
        return process;
    }

    private Process getAssignedProcess(Long processId) {
        Process process = processRepository.findById(processId)
            .orElseThrow(() -> new InvalidRequestException("Process not found"));
        User operator = securityFacade.getCurrentUser();
        if (process.getTask().getOperator().filter(operator::equals).isEmpty()) {
            throw new InvalidRequestException("Process is not assigned to current operator");
        }
        return process;
    }

    private void validatePickedQuantityForProcess(Process process, Integer pickedQuantity) {
        if (pickedQuantity == null || pickedQuantity < 1) {
            throw new InvalidRequestException("Picked quantity must be greater than 0");
        }
        if (pickedQuantity > process.getQuantity()) {
            throw new InvalidRequestException("Picked quantity cannot exceed required quantity");
        }
        if (process.getTask().getTaskType() == TaskType.PICKING_ORDER && !pickedQuantity.equals(process.getQuantity())) {
            throw new InvalidRequestException("Picked quantity must match required quantity for picking tasks");
        }
        if (process.getStock().getQuantity() < pickedQuantity) {
            throw new InvalidRequestException("Not enough stock available");
        }
    }

    private void autoAdvanceGroupedPickingFlow(Process completedProcess) {
        if (completedProcess.getTask().getTaskType() != TaskType.PICKING_ORDER) {
            return;
        }

        orderLineRepository.findByTaskId(completedProcess.getTask().getId()).ifPresent(orderLine -> {
            List<Process> orderProcesses = processRepository.findAllByOrder(orderLine.getOrder());
            pickingFlowService.findNextExecutableProcessAfter(orderProcesses, completedProcess)
                .ifPresent(nextProcess -> {
                    if (nextProcess.getStatus() == Status.ASSIGNED) {
                        nextProcess.setStatus(Status.IN_PROGRESS);
                    }

                    boolean sameSourceLocation = nextProcess.getStock().getLocation().getId()
                        .equals(completedProcess.getStock().getLocation().getId());

                    if (sameSourceLocation) {
                        nextProcess.setSourceLocationScanned(true);
                    }

                    processRepository.save(nextProcess);
                });
        });
    }

    private ProcessExecutionResponse toResponse(Process process) {
        return new ProcessExecutionResponse(
            process.getId(),
            process.getStatus().name(),
            process.isSourceLocationScanned(),
            process.isProductScanned(),
            process.getQuantity(),
            process.getPickedQuantity()
        );
    }
}
