package com.isd.wms.controller;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.service.ReplenishmentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/replenishments")
@RequiredArgsConstructor
public class ReplenishmentController {

    private final ReplenishmentService replenishmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ReplenishmentResponse> createReplenishment(@Valid @RequestBody ReplenishmentCreateRequest request) {
        log.info("REST request to create Replenishment: Product ID = {}, Request. Qty: {}, to Location ID: {}", request.productId(), request.requestedQuantity(), request.destinationLocationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(replenishmentService.createReplenishment(request));
    }

    @GetMapping
    public ResponseEntity<List<ReplenishmentResponse>> getAllReplenishments() {
        return ResponseEntity.ok(replenishmentService.getAllReplenishments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReplenishmentResponse> getReplenishmentById(@PathVariable Long id) {
        return ResponseEntity.ok(replenishmentService.getReplenishmentById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ReplenishmentResponse> updateReplenishment(@PathVariable Long id, @Valid @RequestBody ReplenishmentUpdateRequest request) {
        log.info("REST request to update Replenishment with ID: {}", id);
        return ResponseEntity.ok(replenishmentService.updateReplenishment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<Void> deleteReplenishment(@PathVariable Long id) {
        log.warn("REST request to DELETE Replenishment with ID: {}", id);
        replenishmentService.deleteReplenishment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/filter")
    public ResponseEntity<List<ReplenishmentResponse>> searchReplenishments(@ModelAttribute ReplenishmentSearchRequest request) {
        log.info("REST request to search Replenishments with filters: {}", request);
        return ResponseEntity.ok(replenishmentService.searchReplenishments(request));
    }

    @PostMapping("/search")
    public ResponseEntity<List<ReplenishmentResponse>> searchReplenishmentsFromBody(@RequestBody ReplenishmentSearchRequest request) {
        return ResponseEntity.ok(replenishmentService.searchReplenishments(request));
    }
}
