package com.isd.wms.controller;

import com.isd.wms.dto.process.BarcodeScanRequest;
import com.isd.wms.dto.process.ConfirmPickedQuantityRequest;
import com.isd.wms.dto.process.ProcessExecutionResponse;
import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.service.ProcessExecutionService;
import com.isd.wms.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final ProcessExecutionService processExecutionService;

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('OPERATOR', 'SUPERVISOR', 'DEV')")
    public List<ProcessResponse> getAvailableProcesses() {
        return processService.getAvailableProcesses();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public List<ProcessResponse> getMyProcesses() {
        return processService.getMyProcesses();
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public List<ProcessExecutionResponse> getAssignedProcesses() {
        return processExecutionService.getAssignedProcesses();
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessResponse assignProcess(@PathVariable Long id) {
        return processService.assignProcess(id);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessResponse completeProcess(@PathVariable Long id) {
        return processService.completeProcess(id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse startProcess(@PathVariable Long id) {
        return processExecutionService.startProcess(id);
    }

    @PostMapping("/{id}/scan-location")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse scanSourceLocation(@PathVariable Long id, @RequestBody BarcodeScanRequest request) {
        return processExecutionService.scanSourceLocation(id, request);
    }

    @PostMapping("/{id}/scan-product")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse scanProduct(@PathVariable Long id, @RequestBody BarcodeScanRequest request) {
        return processExecutionService.scanProduct(id, request);
    }

    @PostMapping("/{id}/confirm-quantity")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse confirmPickedQuantity(@PathVariable Long id, @RequestBody ConfirmPickedQuantityRequest request) {
        return processExecutionService.confirmPickedQuantity(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse completeAssignedProcess(@PathVariable Long id) {
        return processExecutionService.completeProcess(id);
    }
}
