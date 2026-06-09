package com.isd.wms.service;

import com.isd.wms.dto.order.*;
import com.isd.wms.entity.Order;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.mapper.ExtendedOrderMapper;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.OrderRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderService {
    private ExtendedOrderMapper extendedOrderMapper;
    private OrderMapper orderMapper;
    private OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        validateOrderRequest(request.logicId());
        Order order = Order.builder()
                .logicId(request.logicId())
                .build();
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        validateOrderRequest(id, request.logicId(), request.status());

        Order order = getOrder(id);

        order.setLogicId(request.logicId());
        order.setStatus(request.status());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrderById(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public OrderResponse getOrderById(@NonNull Long orderId) {
        return orderMapper.toResponse(getOrder(orderId));
    }

    public Order getOrder(@NonNull Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public ExtendedOrderResponse getExtendedOrderById(Long id) {
        Order order = getOrder(id);
        return extendedOrderMapper.toResponse(order);
    }

    public Optional<List<OrderResponse>> searchOrders(OrderSearchRequest request) {
        return Optional.of(orderRepository.filter(request.logicId(), request.status(), request.createdAt(), request.updatedAt()).stream()
                .map(orderMapper::toResponse)
                .toList());
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

    public List<ExtendedOrderResponse> getAllExtendedOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(extendedOrderMapper::toResponse)
                .toList();
    }
}
