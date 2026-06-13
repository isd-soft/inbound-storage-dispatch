package com.isd.wms.controller;


import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.service.OrderLineService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order-lines")
@RequiredArgsConstructor
public class OrderLineController {
    private final OrderLineService orderLineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<OrderLineResponse>> getAllOrderLines() {
        return ResponseEntity.ok(orderLineService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<OrderLineResponse> getOrderLineById(@PathVariable Long id) {
        return ResponseEntity.ok(orderLineService.getOrderLineById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<OrderLineResponse> updateOrderLine(@PathVariable Long id, @Valid @NonNull OrderLineUpdateRequest request) {
        return ResponseEntity.ok(orderLineService.updateOrderLine(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> deleteOrderLine(@PathVariable Long id) {
        orderLineService.deleteOrderLine(id);
        return ResponseEntity.noContent().build();
    }
}
