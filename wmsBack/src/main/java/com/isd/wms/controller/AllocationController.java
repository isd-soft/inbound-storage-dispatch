package com.isd.wms.controller;

import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.dto.allocation.*;
import com.isd.wms.repository.projections.AllocationSupervisorProjection;
import com.isd.wms.service.AllocationExecutionService;
import com.isd.wms.service.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;
    private final AllocationExecutionService allocationExecutionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public List<AllocationSupervisorProjection> getAllAllocations() {
        return allocationService.getAllAllocations();
    }

    @GetMapping("/operators")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public AllocationOperatorResponse getAllocationsOperator() {
        return allocationService.getAllocationsOperator();
    }

    @GetMapping("/operator/current/summary")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<OperatorTaskSummaryResponse> getCurrentSummary() {
        return allocationExecutionService.getCurrentSummary()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/operator/current/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<OperatorTaskSummaryResponse> startCurrentTask() {
        return ResponseEntity.ok(allocationExecutionService.startCurrentTask());
    }

    @PostMapping("/operator/current/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<Void> completeCurrentOrder() {
        allocationExecutionService.completeCurrentOrder();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<String> startAllocation() {
        return ResponseEntity.ok("Allocation started with id: " + allocationExecutionService.startAllocation());
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<AllocationExecutionResponse> scanSourceLocation(@PathVariable Long id, @Valid @RequestBody BarcodeScanRequest request) {
        return ResponseEntity.ok(allocationExecutionService.scanSourceLocation(id, request));
    }

    @PostMapping("/{id}/product")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<AllocationExecutionResponse> scanProduct(@PathVariable Long id, @Valid @RequestBody BarcodeScanRequest request) {
        return ResponseEntity.ok(allocationExecutionService.scanProduct(id, request));
    }

    @PostMapping("/{id}/confirm-quantity")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public AllocationExecutionResponse confirmPickedQuantity(@PathVariable Long id, @Valid @RequestBody ConfirmPickedQuantityRequest request) {
        return allocationExecutionService.confirmPickedQuantity(id, request);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public AllocationCompletionResponse completeAssignedAllocation(@PathVariable Long id) {
        return allocationExecutionService.completeAssignedAllocation(id);
    }
}
