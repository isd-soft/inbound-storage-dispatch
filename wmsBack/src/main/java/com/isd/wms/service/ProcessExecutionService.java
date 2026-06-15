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
        return processRepository.findByOperatorAndStatuses(
                operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public Long startProcess() {
        String currentUsername = securityFacade.getCurrentUsername();
        return processRepository.findOldestAssignedProcessId(currentUsername)
            .orElseThrow(() -> new ProcessesNotFoundException(currentUsername));
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
            throw new InvalidRequestException("Wrong product Barcode");
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

        inventoryService.recordPickingHistory(process.getStock(), process.getPickedQuantity(), operator);

        ProcessCompletionResult result = workflowService.executeProcessCompletion(savedProcess);

        log.info("Process {} completed by operator {}", processId, operator.getUsername());
        return new ProcessCompletionResponse(result);
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
