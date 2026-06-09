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
@Transactional(readOnly = true)
public class OrderService {
    private final ExtendedOrderMapper extendedOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        Order order = new Order(request.logicId());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        if (!request.status().equals(OrderStatus.CREATED)) {
            throw new InvalidRequestException("Order status must be CREATED");
        }
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

    public List<OrderResponse> searchOrders(OrderSearchRequest request) {
        return orderRepository.filter(request.logicId(), request.status(), request.createdAt(), request.updatedAt()).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<ExtendedOrderResponse> getAllExtendedOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(extendedOrderMapper::toResponse)
                .toList();
    }
}
