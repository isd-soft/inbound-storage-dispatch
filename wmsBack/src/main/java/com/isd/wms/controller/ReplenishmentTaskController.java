package com.isd.wms.controller;

import com.isd.wms.dto.replenishment.ReplenishmentTaskCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskResponse;
import com.isd.wms.dto.replenishment.ReplenishmentTaskSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskUpdateRequest;
import com.isd.wms.service.ReplenishmentTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replenishment-tasks")
public class ReplenishmentTaskController {
    private final ReplenishmentTaskService replenishmentTaskService;

    public ReplenishmentTaskController(ReplenishmentTaskService replenishmentTaskService) {
        this.replenishmentTaskService = replenishmentTaskService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'DEV')")
    public ResponseEntity<ReplenishmentTaskResponse> createReplenishmentTask(@Valid @RequestBody ReplenishmentTaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(replenishmentTaskService.createReplenishmentTask(request));
    }

    @GetMapping
    public List<ReplenishmentTaskResponse> getAllReplenishmentTasks() {
        return replenishmentTaskService.getAllReplenishmentTasks();
    }

    @GetMapping("/{id}")
    public ReplenishmentTaskResponse getReplenishmentTaskById(@PathVariable Long id) {
        return replenishmentTaskService.getReplenishmentTaskById(id);
    }

    @PutMapping("/{id}")
    public ReplenishmentTaskResponse updateReplenishmentTask(@PathVariable Long id, @Valid @RequestBody ReplenishmentTaskUpdateRequest request) {
        return replenishmentTaskService.updateReplenishmentTask(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReplenishmentTask(@PathVariable Long id) {
        replenishmentTaskService.deleteReplenishmentTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ReplenishmentTaskResponse> searchReplenishmentTasks(@RequestBody ReplenishmentTaskSearchRequest request) {
        return replenishmentTaskService.searchReplenishmentTasks(request);
    }
}
