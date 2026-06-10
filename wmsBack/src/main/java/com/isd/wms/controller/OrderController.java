package com.isd.wms.controller;

import com.isd.wms.dto.order.*;
import com.isd.wms.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private OrderService orderService;

    @RequestMapping("/")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<OrderResponse> getOrders() {
        return orderService.getAllOrders();
    }

    @RequestMapping("/extended")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<ExtendedOrderResponse> getExtendedOrders() {
        return orderService.getAllExtendedOrders();
    }

    @RequestMapping("/{id}")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @RequestMapping("/{id}/extended")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ExtendedOrderResponse getExtendedOrderById(@PathVariable Long id) {
        return orderService.getExtendedOrderById(id);
    }

    @RequestMapping("/")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public OrderResponse createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request);
    }

    @RequestMapping("/{id}")
    @PutMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public OrderResponse updateOrder(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        return orderService.updateOrder(id, request);
    }

    @RequestMapping("/{id}")
    @DeleteMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public void deleteOrder(@PathVariable Long id) {
        orderService.deleteOrderById(id);
    }

    @RequestMapping("/")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public Optional<List<OrderResponse>> searchOrders(OrderSearchRequest request) {
        return orderService.searchOrders(request);
    }

}
