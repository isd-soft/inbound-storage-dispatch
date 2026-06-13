package com.isd.wms.service;

import com.isd.wms.dto.order.*;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.ExtendedOrderMapper;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Slf4j
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
    private final TaskRepository taskRepository;

    @Transactional
    public OrderResponse addExtendedOrder(ExtendedOrderCreateRequest request) {
        log.info("Processing inbound Extended Order creation. Logic ID: '{}', total lines requested: {}",
            request.order().logicId(), request.lines().size());

        Order order = addOrder(request.order());
        for (OrderLineCreateRequest oRequest: request.lines()) {
            oRequest = new OrderLineCreateRequest(oRequest, order.getId());
            orderLineService.addOrderLine(order, oRequest);
        }

        log.info("Extended Order successfully created. Assigned System ID: {}, Logic ID: '{}'", order.getId(), order.getLogicId());
        return orderMapper.toResponse(order);
    }

    @Transactional
    public Order addOrder(OrderCreateRequest request) {
        log.debug("Persisting base Order record. Logic ID: '{}', Destination Location ID: {}", request.logicId(), request.destinationLocationId());
        Order order = new Order(request.logicId(), getLocation(request.destinationLocationId()));
        return orderRepository.save(order);
    }

    @Transactional
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        log.info("Request to update Order ID: {}. New Status target: {}", id, request.status());

        if (!request.status().equals(Status.CREATED)) {
            log.warn("Order update rejected for ID: {}. Status change to '{}' is invalid (Must be CREATED)", id, request.status());
            throw new InvalidRequestException("Order status must be CREATED");
        }

        Order order = getOrder(id);
        String oldLogicId = order.getLogicId();

        order.setLogicId(request.logicId());
        order.setStatus(request.status());

        Order savedOrder = orderRepository.save(order);
        log.info("Order ID: {} successfully updated. Logic ID: '{}' -> '{}', New Status: {}",
            id, oldLogicId, savedOrder.getLogicId(), savedOrder.getStatus());

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public void deleteOrderById(@PathVariable Long id) {
        log.warn("HARD DELETE requested for Order ID: {}", id);
        if (!orderRepository.existsById(id)) {
            log.warn("Delete aborted. Order ID: {} does not exist in database", id);
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteById(id);
        log.warn("Order ID: {} was permanently deleted from the system", id);
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
            .orElseThrow(() -> {
                log.warn("Order lookup failed. Order ID {} not found", orderId);
                return new OrderNotFoundException(orderId);
            });
    }

    @Transactional
    public void assignOrder(Long orderId, Long operatorId) {
        log.info("Initiating Order assignment. Order ID: {} -> Operator ID: {}", orderId, operatorId);

        Order order = getOrder(orderId);
        User operator = getUser(operatorId);

        List<Task> tasks = getAllTasksByOrder(order);

        if (tasks.isEmpty()) {
            log.warn("Potential configuration issue: Order ID {} has 0 tasks associated. No tasks were assigned to Operator '{}'", orderId, operator.getUsername());
        } else {
            log.info("Assigning {} tasks from Order ID: {} to Operator: '{}'", tasks.size(), orderId, operator.getUsername());
        }

        tasks.forEach((t)-> t.setOperator(operator));
        taskRepository.saveAll(tasks);

        log.info("Order ID: {} successfully assigned to Operator '{}'", orderId, operator.getUsername());
    }

    private List<Task> getAllTasksByOrder(Order order) {
        return taskRepository.findAllByOrder(order);
    }

    public Order getOldestOrderAssignedToOperator(User operator) {
        log.debug("Fetching oldest active order for operator: '{}'", operator.getUsername());
        return orderRepository.findOldestOrderAssignedToOperator(operator.getId())
            .orElseThrow(() -> {
                log.debug("No active assigned orders found for operator ID: {}", operator.getId());
                return new OrderNotFoundException(operator.getId());
            });
    }

    private User getUser(Long operatorId) {
        return userRepository.findById(operatorId)
            .orElseThrow(() -> {
                log.warn("User lookup failed in Order context. Operator ID {} not found", operatorId);
                return new UserNotFoundException(operatorId);
            });
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> {
                log.warn("Location lookup failed in Order context. Location ID {} not found", locationId);
                return new LocationNotFoundException(locationId);
            });
    }

    public ExtendedOrderResponse getExtendedOrderById(Long id) {
        Order order = getOrder(id);
        return extendedOrderMapper.toResponse(order);
    }

    public List<OrderResponse> searchOrders(OrderSearchRequest request) {
        log.debug("Executing dynamic order search/filter with criteria: logicId='{}', status={}", request.logicId(), request.status());
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
