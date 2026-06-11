package com.isd.wms.service;

import com.isd.wms.dto.order.*;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.ExtendedOrderMapper;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final ExtendedOrderMapper extendedOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final LocationRepository locationRepository;
    private final OrderLineService orderLineService;
    private final UserRepository userRepository;
    private final ProcessRepository processRepository;

    @Transactional
    public OrderResponse addExtendedOrder(ExtendedOrderCreateRequest request) {
        Order order = addOrder(request.order());
        for (OrderLineCreateRequest oRequest: request.lines()) {
            oRequest = new OrderLineCreateRequest(oRequest, order.getId());
            orderLineService.addOrderLine(order, oRequest);
        }
        return orderMapper.toResponse(order);
    }

    @Transactional
    public Order addOrder(OrderCreateRequest request) {
        Order order = new Order(request.logicId(), getLocation(request.destinationLocationId()));
        return orderRepository.save(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        if (!request.status().equals(Status.CREATED)) {
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

    @Transactional
    public void assignOrder(Long orderId, Long operatorId) {
        Order order = getOrder(orderId);
        User operator = getUser(operatorId);

        List<Process> processes = getProcessesAllByOrder(order);

        processes.forEach((p)-> p.setOperator(operator));
    }

    private List<Process> getProcessesAllByOrder(Order order) {
        return processRepository.findAllByOrder(order);
    }

    public Order getOldestOrder(User operator) {
        return orderRepository
            .findOldestOrderByOperator(operator, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new OrderNotFoundException(operator.getId()));
    }

    private User getUser(Long operatorId) {
        return userRepository.findById(operatorId)
            .orElseThrow(() -> new UserNotFoundException(operatorId));
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }

    public ExtendedOrderResponse getExtendedOrderById(Long id) {
        Order order = getOrder(id);
        return extendedOrderMapper.toResponse(order);
    }

    public List<OrderResponse> searchOrders(OrderSearchRequest request) {
        return orderRepository.filter(request.logicId(), request.destinationLocationId(), request.status(), request.createdAt(), request.updatedAt()).stream()
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
