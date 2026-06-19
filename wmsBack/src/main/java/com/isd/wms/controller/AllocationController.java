package com.isd.wms.controller;

import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.dto.allocation.*;
import com.isd.wms.dto.transport_unit.ScanTuRequest;
import com.isd.wms.dto.transport_unit.TaskActionResponse;
import com.isd.wms.repository.projections.AllocationSupervisorProjection;
import com.isd.wms.service.AllocationExecutionService;
import com.isd.wms.service.AllocationService;
import com.isd.wms.service.TransportUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;
    private final AllocationExecutionService allocationExecutionService;
    private final TransportUnitService tuService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
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
        log.info("Operator requested start for current task");
        return ResponseEntity.ok(allocationExecutionService.startCurrentTask());
    }

    @PostMapping("/{id}/scan-tu")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<OperatorTaskSummaryResponse> scanTransportUnit(
        @PathVariable Long id,
        @Valid @RequestBody ScanTuRequest request) {

        log.info("Received TU scan for Allocation ID: {}. Barcode: {}", id, request.barcode());

        tuService.occupyTransportUnit(request.barcode(), id, request.isOrder());

        OperatorTaskSummaryResponse updatedSummary = allocationExecutionService.getCurrentSummary()
            .orElseThrow(() -> new IllegalStateException("Could not generate summary after scanning TU"));

        return ResponseEntity.ok(updatedSummary);
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<TaskActionResponse> dispatchAllocation(
        @PathVariable Long id,
        @RequestParam String currentBarcode) {

        log.info("Initiating DISPATCH process for Allocation ID: {}, linked with TU: {}", id, currentBarcode);
        tuService.releaseTransportUnit(currentBarcode);

        TaskActionResponse response = new TaskActionResponse(
            "COMPLETED",
            false,
            null,
            "Drop TU at dispatch",
            "Task completed successfully. Transport Unit has been released."
        );
        return ResponseEntity.ok(response);
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
