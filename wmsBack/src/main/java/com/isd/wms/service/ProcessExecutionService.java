package com.isd.wms.service;

import com.isd.wms.dto.process.BarcodeScanRequest;
import com.isd.wms.dto.process.ConfirmPickedQuantityRequest;
import com.isd.wms.dto.process.ProcessExecutionResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessExecutionService {

    private final ProcessRepository processRepository;
    private final StockRepository stockRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public List<ProcessExecutionResponse> getAssignedProcesses() {
        User operator = getCurrentUser();
        return processRepository.findByOperatorAndStatuses(
                        operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProcessExecutionResponse startProcess(Long processId) {
        log.info("Starting process {}", processId);
        Process process = getAssignedProcess(processId);

        if (process.getStatus() == Status.COMPLETED) {
            throw new InvalidRequestException("Process is already completed");
        }
        if (process.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Process is cancelled");
        }
        if (process.getStatus() != Status.ASSIGNED && process.getStatus() != Status.CREATED) {
            throw new InvalidRequestException("Process cannot be started from status " + process.getStatus());
        }

        process.setStatus(Status.IN_PROGRESS);
        return toResponse(processRepository.save(process));
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
            throw new InvalidRequestException("Wrong product/SKU barcode");
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
    public ProcessExecutionResponse completeProcess(Long processId) {
        Process process = getAssignedProcessInProgress(processId);
        User operator = getCurrentUser();

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

        Stock sourceStock = process.getStock();
        int pickedQuantity = process.getPickedQuantity();
        sourceStock.setQuantity(sourceStock.getQuantity() - pickedQuantity);
        sourceStock.setReservedQuantity(Math.max(0, sourceStock.getReservedQuantity() - process.getQuantity()));
        stockRepository.save(sourceStock);

        process.setStatus(Status.COMPLETED);
        Process savedProcess = processRepository.save(process);

        inventoryService.recordPickingHistory(sourceStock, pickedQuantity, operator);
        updateParentStatuses(savedProcess);

        log.info("Process {} completed by operator {}", processId, operator.getUsername());
        return toResponse(savedProcess);
    }

    private void updateParentStatuses(Process process) {
        Task task = process.getTask();
        List<Process> taskProcesses = processRepository.findAllByTaskId(task.getId());
        boolean taskCompleted = taskProcesses.stream()
                .allMatch(taskProcess -> taskProcess.getStatus() == Status.COMPLETED
                        || taskProcess.getId().equals(process.getId()));

        if (!taskCompleted) {
            return;
        }

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        orderLineRepository.findByTaskId(task.getId()).ifPresent(orderLine -> {
            orderLine.setStatus(Status.COMPLETED);
            orderLineRepository.save(orderLine);
            updateOrderStatus(orderLine.getOrder());
        });
    }

    private void updateOrderStatus(Order order) {
        boolean orderCompleted = order.getOrderLines().stream()
                .allMatch(orderLine -> orderLine.getStatus() == Status.COMPLETED);

        if (orderCompleted) {
            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);
        }
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
        User operator = getCurrentUser();
        if (process.getTask().getOperator().filter(operator::equals).isEmpty()) {
            throw new InvalidRequestException("Process is not assigned to current operator");
        }
        return process;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private void validatePickedQuantityForProcess(Process process, Integer pickedQuantity) {
        if (pickedQuantity > process.getQuantity()) {
            throw new InvalidRequestException("Picked quantity cannot exceed required quantity");
        }
        if (process.getStock().getQuantity() < pickedQuantity) {
            throw new InvalidRequestException("Not enough stock available");
        }
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
