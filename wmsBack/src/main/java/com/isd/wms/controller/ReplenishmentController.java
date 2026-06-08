package com.isd.wms.controller;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.service.ReplenishmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replenishment-tasks")
public class ReplenishmentController {
    private final ReplenishmentService replenishmentService;

    public ReplenishmentController(ReplenishmentService replenishmentService) {
        this.replenishmentService = replenishmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ReplenishmentResponse> createReplenishmentTask(@Valid @RequestBody ReplenishmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(replenishmentService.createReplenishment(request));
    }

    @GetMapping
    public List<ReplenishmentResponse> getAllReplenishmentTasks() {
        return replenishmentService.getAllReplenishments();
    }

    @GetMapping("/{id}")
    public ReplenishmentResponse getReplenishmentTaskById(@PathVariable Long id) {
        return replenishmentService.getReplenishmentById(id);
    }

    @PutMapping("/{id}")
    public ReplenishmentResponse updateReplenishmentTask(@PathVariable Long id, @Valid @RequestBody ReplenishmentUpdateRequest request) {
        return replenishmentService.updateReplenishment(id, request);
    }

//    @PatchMapping("/{id}")
//    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
//    public ReplenishmentResponse assignReplenishmentTask(@PathVariable Long id) {
//        return replenishmentService.assignReplenishment(id);
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReplenishmentTask(@PathVariable Long id) {
        replenishmentService.deleteReplenishment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ReplenishmentResponse> searchReplenishmentTasks(@RequestBody ReplenishmentSearchRequest request) {
        return replenishmentService.searchReplenishments(request);
    }
}
