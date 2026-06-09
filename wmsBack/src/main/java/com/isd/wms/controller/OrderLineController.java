package com.isd.wms.controller;


import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.service.OrderLineService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order-lines")
@RequiredArgsConstructor
public class OrderLineController {
    private final OrderLineService orderLineService;

    @GetMapping
    public ResponseEntity<List<OrderLineResponse>> getAllOrderLines() {
        return ResponseEntity.ok(orderLineService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderLineResponse> getOrderLineById(@PathVariable Long id) {
        return ResponseEntity.ok(orderLineService.getOrderLineById(id));
    }

    @PostMapping
    public ResponseEntity<OrderLineResponse> addOrderLine(@RequestBody OrderLineCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderLineService.addOrderLine(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderLineResponse> updateOrderLine(@PathVariable Long id, @NonNull OrderLineUpdateRequest request) {
        return ResponseEntity.ok(orderLineService.updateOrderLine(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrderLine(@PathVariable Long id) {
        orderLineService.deleteOrderLine(id);
        return ResponseEntity.noContent().build();
    }
}
