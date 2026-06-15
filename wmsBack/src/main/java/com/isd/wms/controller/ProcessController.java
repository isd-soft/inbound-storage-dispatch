package com.isd.wms.controller;

import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.dto.process.*;
import com.isd.wms.service.ProcessExecutionService;
import com.isd.wms.service.ProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ProcessOperatorResponse getAllProcesses() {
        return processService.getProcessesOperator();
    }

    @GetMapping("/operator/current/summary")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<OperatorTaskSummaryResponse> getCurrentSummary() {
        return processExecutionService.getCurrentSummary()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/operator/current/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<OperatorTaskSummaryResponse> startCurrentTask() {
        return ResponseEntity.ok(processExecutionService.startCurrentTask());
    }

    @PostMapping("/operator/current/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<Void> completeCurrentOrder() {
        processExecutionService.completeCurrentOrder();
        return ResponseEntity.noContent().build();
    }

//    @GetMapping("/my")
//    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
//    public List<ProcessOperatorResponse> getMyProcesses() {
//        return processService.getMyProcesses();
//    }

//    @PatchMapping("/{id}/complete")
//    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
//    public ProcessResponse completeProcess(@PathVariable Long id) {
//        return processService.completeProcess(id);
//    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<String> startProcess() {
        return ResponseEntity.ok("Process started with id: " + processExecutionService.startProcess());
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
    public ProcessCompletionResponse completeAssignedProcess(@PathVariable Long id) {
        return processExecutionService.completeAssignedProcess(id);
    }
}
