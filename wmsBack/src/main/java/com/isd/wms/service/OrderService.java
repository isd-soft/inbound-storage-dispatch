package com.isd.wms.service;

import com.isd.wms.dto.order.OrderCreateRequest;
import com.isd.wms.dto.order.OrderResponse;
import com.isd.wms.dto.order.OrderUpdateRequest;
import com.isd.wms.entity.Order;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderService {
    private OrderMapper orderMapper;
    private OrderRepository orderRepository;
    private OrderLineRepository orderLineRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        validateOrderRequest(request.logicId());
        Order order = Order.builder()
                .logicId(request.logicId())
                .build();
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional OrderResponse updateOrder(OrderUpdateRequest request) {
        validateOrderRequest(request.orderId(), request.logicId(), request.status());

        Order order = orderRepository.getById(request.orderId());

        order.setLogicId(request.logicId());
        order.setStatus(request.status());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    private Order getOrder(@NonNull Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void validateOrderRequest(@NonNull Long id, @NonNull String s, @NonNull OrderStatus status) {
        if (s.isEmpty()) {
            throw new InvalidRequestException("Order logic id cannot be empty");
        }
    }

    private void validateOrderRequest(@NonNull String s) {
        if (s.isEmpty()) {
            throw new InvalidRequestException("Order logic id cannot be empty");
        }
    }

}
