package com.isd.wms.controller;

import com.isd.wms.dto.process.*;
import com.isd.wms.service.ProcessExecutionService;
import com.isd.wms.service.ProcessService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final ProcessExecutionService processExecutionService;

    @GetMapping("/operators")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessOperatorResponse getAllProcesses() {
        return processService.getProcessesOperator();
    }

//    @GetMapping("/my")
//    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
//    public List<ProcessOperatorResponse> getMyProcesses() {
//        return processService.getMyProcesses();
//    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessResponse completeProcess(@PathVariable Long id) {
        log.info("REST request to patch complete Process with ID: {}", id);
        return processService.completeProcess(id);
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<String> startProcess() {
        log.info("REST request to START a new process execution");
        Long processId = processExecutionService.startProcess();
        log.info("Process successfully started with ID: {}", processId);
        return ResponseEntity.ok("Process started with id: " + processId);
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse scanSourceLocation(@PathVariable Long id, @Valid @RequestBody BarcodeScanRequest request) {
        log.info("REST request for Process ID: {} - Scanned Source Location Barcode: {}", id, request.barcode());
        return processExecutionService.scanSourceLocation(id, request);
    }

    @PostMapping("/{id}/product")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse scanProduct(@PathVariable Long id, @Valid @RequestBody BarcodeScanRequest request) {
        log.info("REST request for Process ID: {} - Scanned Product Barcode: {}", id, request.barcode());
        return processExecutionService.scanProduct(id, request);
    }

    @PostMapping("/{id}/confirm-quantity")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse confirmPickedQuantity(@PathVariable Long id, @Valid @RequestBody ConfirmPickedQuantityRequest request) {
        log.info("REST request for Process ID: {} - Confirming Picked Quantity: {}", id, request.pickedQuantity());
        return processExecutionService.confirmPickedQuantity(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse completeAssignedProcess(@PathVariable Long id) {
        log.info("REST request to COMPLETE Assigned Process ID: {}", id);
        return processExecutionService.completeProcess(id);
    }
}
