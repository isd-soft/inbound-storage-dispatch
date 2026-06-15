package com.isd.wms.service;

import com.isd.wms.dto.process.*;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProcessesNotFoundException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessExecutionService {

    private final ProcessRepository processRepository;
    private final StockRepository stockRepository;
    private final InventoryService inventoryService;
    private final SecurityFacade securityFacade;
    private final WorkflowService workflowService;

    public List<ProcessExecutionResponse> getAssignedProcesses() {
        User operator = securityFacade.getCurrentUser();
        log.debug("Fetching assigned and in-progress processes for operator: '{}'", operator.getUsername());
        return processRepository.findByOperatorAndStatuses(
                operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public Long startProcess() {
        String currentUsername = securityFacade.getCurrentUsername();
        log.info("Operator '{}' requested the next available process initialization", currentUsername);
        return processRepository.findOldestAssignedProcessId(currentUsername)
            .orElseThrow(() -> {
                log.warn("No assigned processes found for operator '{}'", currentUsername);
                return new ProcessesNotFoundException(currentUsername);
            });
    }

    @Transactional
    public ProcessExecutionResponse scanSourceLocation(Long processId, BarcodeScanRequest request) {
        Process process = getAssignedProcessInProgress(processId);
        String barcode = request.barcode().trim();
        String expectedBarcode = process.getStock().getLocation().getBarcode();

        log.info("Process ID {}: Location barcode scanned. Scanned='{}', Expected='{}'", processId, barcode, expectedBarcode);

        if (!expectedBarcode.equals(barcode)) {
            log.warn("Process ID {} validation failed: Wrong source location barcode. Scanned='{}', Expected='{}'", processId, barcode, expectedBarcode);
            throw new InvalidRequestException("Wrong source location barcode");
        }

        process.setSourceLocationScanned(true);
        log.info("Process ID {}: Source location validation successful", processId);
        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessExecutionResponse scanProduct(Long processId, BarcodeScanRequest request) {
        Process process = getAssignedProcessInProgress(processId);
        if (!process.isSourceLocationScanned()) {
            log.warn("Process ID {} workflow violation: Attempted to scan product before scanning source location", processId);
            throw new InvalidRequestException("Source location must be scanned first");
        }

        String barcode = request.barcode().trim();
        Stock expectedStock = process.getStock();
        Product expectedProduct = expectedStock.getProduct()
            .filter(product -> product.getBarcode() != null && product.getBarcode().equalsIgnoreCase(barcode))
            .orElse(null);
            
        if (expectedProduct == null) {
            log.warn("Process ID {} validation failed: Wrong product barcode scanned. Value='{}'", processId, barcode);
            throw new InvalidRequestException("Wrong product barcode");
        }

        stockRepository.findByProductIdAndLocationId(
                expectedProduct.getId(),
                expectedStock.getLocation().getId())
            .orElseThrow(() -> {
                log.error("CRITICAL: Stock mismatch for Stock ID {} during process {}. Record missing in DB!", expectedStock.getId(), processId);
                return new StockNotFoundException(expectedStock.getId());
            });

        process.setProductScanned(true);
        log.info("Process ID {}: Product validation successful for barcode: '{}'", processId, expectedProduct.getName());
        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessExecutionResponse confirmPickedQuantity(Long processId, ConfirmPickedQuantityRequest request) {
        Process process = getAssignedProcessInProgress(processId);

        if (!process.isProductScanned()) {
            log.warn("Process ID {} workflow violation: Attempted to confirm quantity before scanning product", processId);
            throw new InvalidRequestException("Product barcode must be scanned first");
        }

        Integer pickedQuantity = request.pickedQuantity();
        log.info("Process ID {}: Quantity confirmation requested. Picked Quantity={}, Target Quantity={}",
            processId, pickedQuantity, process.getQuantity());

        try {
            validatePickedQuantityForProcess(process, pickedQuantity);
        } catch (InvalidRequestException e) {
            log.warn("Process ID {} quantity validation failed: {} (Requested={}, Available={})",
                processId, e.getMessage(), pickedQuantity, process.getStock().getQuantity());
            throw e;
        }

        process.setPickedQuantity(pickedQuantity);
        log.info("Process ID {}: Picked quantity {} confirmed successfully", processId, pickedQuantity);
        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessCompletionResponse completeProcess(Long processId) {
        Process process = getAssignedProcessInProgress(processId);
        User operator = securityFacade.getCurrentUser();

        log.info("Process ID {}: Completion request triggered by operator '{}'", processId, operator.getUsername());

        if (!process.isSourceLocationScanned()) {
            log.warn("Process completion rejected for Process ID: {}. Reason: Source location has not been scanned yet. [Current State: ProductScanned={}, QtyConfirmed={}]",
                processId, process.isProductScanned(), (process.getPickedQuantity() != null));
            throw new InvalidRequestException("Source location must be scanned first");
        }
        if (!process.isProductScanned()) {
            log.warn("Process completion rejected for Process ID: {}. Reason: Product barcode has not been scanned yet. [Current State: LocationScanned=true, QtyConfirmed={}]",
                processId, (process.getPickedQuantity() != null));
            throw new InvalidRequestException("Product barcode must be scanned first");
        }
        if (process.getPickedQuantity() == null) {
            log.warn("Process completion rejected for Process ID: {}. Reason: Picked quantity is not confirmed. [Current State: LocationScanned=true, ProductScanned=true]",
                processId);
            throw new InvalidRequestException("Picked quantity must be confirmed before completion");
        }

        log.debug("Process ID {}: All workflow state validations passed successfully. Proceeding to inventory updates.", processId);
        validatePickedQuantityForProcess(process, process.getPickedQuantity());

        process.setStatus(Status.COMPLETED);
        Process savedProcess = processRepository.save(process);

        inventoryService.recordPickingHistory(process.getStock(), process.getPickedQuantity(), operator);
      
        ProcessCompletionResult result = workflowService.executeProcessCompletion(savedProcess);

        log.info("Process {} completed by operator {}", processId, operator.getUsername());
        return new ProcessCompletionResponse(result);
    }

    private Process getAssignedProcessInProgress(Long processId) {
        Process process = getAssignedProcess(processId);
        if (process.getStatus() == Status.COMPLETED) {
            log.warn("Process operation rejected: Process ID {} is already COMPLETED. Cannot modify a closed process.", processId);
            throw new InvalidRequestException("Process is already completed");
        }
        if (process.getStatus() == Status.CANCELED) {
            log.warn("Process operation rejected: Process ID {} has been CANCELED (possibly by a supervisor).", processId);
            throw new InvalidRequestException("Process is cancelled");
        }
        if (process.getStatus() != Status.IN_PROGRESS) {
            log.warn("Process operation rejected: Process ID {} is in state '{}', but this operation requires it to be IN_PROGRESS.",
                processId, process.getStatus());
            throw new InvalidRequestException("Process is not in progress");
        }
        log.debug("Process ID {} state validation passed. Current status is IN_PROGRESS.", processId);
        return process;
    }

    private Process getAssignedProcess(Long processId) {
        Process process = processRepository.findById(processId)
            .orElseThrow(() -> new InvalidRequestException("Process not found"));
            
        User operator = securityFacade.getCurrentUser();
        
        if (process.getTask().getOperator().filter(operator::equals).isEmpty()) {
            log.warn("Security violation: Operator '{}' tried to execute Process ID {} which is not assigned to them",
                operator.getUsername(), processId);
            throw new InvalidRequestException("Process is not assigned to current operator");
        }
        return process;
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