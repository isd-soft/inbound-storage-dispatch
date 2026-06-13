package com.isd.wms.controller;

import com.isd.wms.dto.order.*;
import com.isd.wms.service.OrderService;
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
        log.info("REST request to create Order: {}", request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.addExtendedOrder(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        log.info("REST request to update Order with ID: {}", id);
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        log.warn("REST request to DELETE Order with ID: {}", id);
        orderService.deleteOrderById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/operators/{operatorId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> assignOrder(@PathVariable Long orderId, @PathVariable Long operatorId) {
        log.info("REST request to assign Order ID: {} to Operator ID: {}", orderId, operatorId);
        orderService.assignOrder(orderId, operatorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<List<OrderResponse>> searchOrders(@ModelAttribute OrderSearchRequest request) {
        log.info("REST request to search Orders with filters: {}", request);
        return ResponseEntity.ok(orderService.searchOrders(request));
    }
}
