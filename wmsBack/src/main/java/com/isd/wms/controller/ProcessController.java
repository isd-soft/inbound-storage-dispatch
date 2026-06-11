package com.isd.wms.controller;

import com.isd.wms.dto.process.BarcodeScanRequest;
import com.isd.wms.dto.process.ConfirmPickedQuantityRequest;
import com.isd.wms.dto.process.ProcessExecutionResponse;
import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.service.ProcessExecutionService;
import com.isd.wms.service.ProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final ProcessExecutionService processExecutionService;

    @GetMapping("/operators")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<ProcessOperatorResponse> getAllProcesses() {
        return processService.getProcessesOperator();
    }

//    @GetMapping("/my")
//    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
//    public List<ProcessOperatorResponse> getMyProcesses() {
//        return processService.getMyProcesses();
//    }

//    @GetMapping
//    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
//    public List<ProcessExecutionResponse> getAssignedProcesses() {
//        return processExecutionService.getAssignedProcesses();
//    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessOperatorResponse completeProcess(@PathVariable Long id) {
        return processService.completeProcess(id);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse startProcess(@PathVariable Long id) {
        return processExecutionService.startProcess(id);
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse scanSourceLocation(@PathVariable Long id, @Valid @RequestBody BarcodeScanRequest request) {
        return processExecutionService.scanSourceLocation(id, request);
    }

    @PostMapping("/{id}/product")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse scanProduct(@PathVariable Long id, @Valid @RequestBody BarcodeScanRequest request) {
        return processExecutionService.scanProduct(id, request);
    }

    @PostMapping("/{id}/confirm-quantity")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse confirmPickedQuantity(@PathVariable Long id, @Valid @RequestBody ConfirmPickedQuantityRequest request) {
        return processExecutionService.confirmPickedQuantity(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessExecutionResponse completeAssignedProcess(@PathVariable Long id) {
        return processExecutionService.completeProcess(id);
    }
}
