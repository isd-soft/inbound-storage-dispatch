package com.isd.wms.controller;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.service.ReplenishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replenishments")
@RequiredArgsConstructor
public class ReplenishmentController {

    private final ReplenishmentService replenishmentService;


    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ReplenishmentResponse> createReplenishment(@Valid @RequestBody ReplenishmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(replenishmentService.createReplenishment(request));
    }

    @GetMapping
    public List<ReplenishmentResponse> getAllReplenishments() {
        return replenishmentService.getAllReplenishments();
    }

    @GetMapping("/{id}")
    public ReplenishmentResponse getReplenishmentById(@PathVariable Long id) {
        return replenishmentService.getReplenishmentById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ReplenishmentResponse updateReplenishment(@PathVariable Long id, @Valid @RequestBody ReplenishmentUpdateRequest request) {
        return replenishmentService.updateReplenishment(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<Void> deleteReplenishment(@PathVariable Long id) {
        replenishmentService.deleteReplenishment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public List<ReplenishmentResponse> searchReplenishments(@RequestBody ReplenishmentSearchRequest request) {
        return replenishmentService.searchReplenishments(request);
    }
}
