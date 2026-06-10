package com.isd.wms.controller;


import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.service.OrderLineService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-lines")
@RequiredArgsConstructor
public class OrderLineController {
    private final OrderLineService orderLineService;

    @RequestMapping("/")
    @GetMapping
    public List<OrderLineResponse> getOrderLines() {
        return orderLineService.getAll();
    }

    @RequestMapping("/{id}")
    @GetMapping
    public OrderLineResponse getOrderLine(@NonNull Long orderLineId) {
        return orderLineService.getOrderLineById(orderLineId);
    }

    @RequestMapping("/")
    @PostMapping
    public OrderLineResponse createOrderLine(@Valid @NonNull OrderLineCreateRequest request) {
        return orderLineService.createOrderLine(request);
    }

    @RequestMapping("/{id}")
    @PutMapping
    public OrderLineResponse updateOrderLine(@NonNull Long orderLineId, @Valid @NonNull OrderLineUpdateRequest request) {
        return orderLineService.updateOrderLine(orderLineId, request);
    }
}
