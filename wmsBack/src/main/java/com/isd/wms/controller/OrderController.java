package com.isd.wms.controller;

import com.isd.wms.dto.order.*;
import com.isd.wms.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<OrderResponse>> getOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/extended")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<ExtendedOrderResponse>> getExtendedOrders() {
        return ResponseEntity.ok(orderService.getAllExtendedOrders());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/extended/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ExtendedOrderResponse> getExtendedOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getExtendedOrderById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody ExtendedOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.addExtendedOrder(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrderById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/operators/{operatorId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> assignOrder(@PathVariable Long orderId, @PathVariable Long operatorId) {
        orderService.assignOrder(orderId, operatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<OrderResponse>> searchOrders(@ModelAttribute OrderSearchRequest request) {
        return ResponseEntity.ok(orderService.searchOrders(request));
    }

    @GetMapping("/shortages")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<com.isd.wms.dto.order.shortage.ShortageOrderResponse>> getShortageOrders() {
        return ResponseEntity.ok(orderService.getShortageOrders());
    }

    @GetMapping("/{id}/shortage-details")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<com.isd.wms.dto.order.shortage.ShortageDetailsResponse> getShortageDetails(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getShortageDetails(id));
    }
}
