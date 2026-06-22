package com.isd.wms.controller;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.dto.replenishment.shortage.ShortageReplenishmentDetailsResponse;
import com.isd.wms.dto.replenishment.shortage.ShortageReplenishmentResponse;
import com.isd.wms.service.ReplenishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        return ResponseEntity.ok(replenishmentService.updateReplenishment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<Void> deleteReplenishment(@PathVariable Long id) {
        replenishmentService.deleteReplenishment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/filter")
    public ResponseEntity<List<ReplenishmentResponse>> searchReplenishments(@ModelAttribute ReplenishmentSearchRequest request) {
        return ResponseEntity.ok(replenishmentService.searchReplenishments(request));
    }

    @PostMapping("/search")
    public ResponseEntity<List<ReplenishmentResponse>> searchReplenishmentsFromBody(@RequestBody ReplenishmentSearchRequest request) {
        return ResponseEntity.ok(replenishmentService.searchReplenishments(request));
    }

    @GetMapping("/shortages")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<ShortageReplenishmentResponse>> getShortageReplenishments() {
        return ResponseEntity.ok(replenishmentService.getShortageReplenishments());
    }

    @GetMapping("/{id}/shortage-details")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ShortageReplenishmentDetailsResponse> getShortageDetails(@PathVariable Long id) {
        return ResponseEntity.ok(replenishmentService.getShortageDetails(id));
    }

    @PostMapping("/{replenishmentId}/operators/{operatorId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> assignReplenishment(@PathVariable Long replenishmentId, @PathVariable Long operatorId) {
        replenishmentService.assignReplenishment(replenishmentId, operatorId);
        return ResponseEntity.ok("Replenishment assigned with success.");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ReplenishmentResponse> cancelReplenishment(@PathVariable Long id) {
        return ResponseEntity.ok(replenishmentService.cancelReplenishment(id));
    }

    @PostMapping("/imports")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> importProducts(@RequestParam("file") MultipartFile file) {
        replenishmentService.importReplenishmentsFromFile(file);
        return ResponseEntity.ok("Replenishments were successfully imported.");
    }
}
