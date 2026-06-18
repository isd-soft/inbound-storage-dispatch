package com.isd.wms.service;

import com.isd.wms.dto.order.*;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.mapper.ExtendedOrderMapper;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.imports.ImportService;
import com.isd.wms.service.imports.xlsx.dto.ExtendedOrderInfo;
import com.isd.wms.service.imports.xlsx.dto.ReplenishmentInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final ExtendedOrderMapper extendedOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final LocationRepository locationRepository;
    private final OrderLineService orderLineService;
    private final AllocationRepository allocationRepository;
    private final TaskRepository taskRepository;
    private final OrderLineRepository orderLineRepository;
    private final TaskService taskService;
    private final ImportService importService;

    @Transactional
    public OrderResponse addExtendedOrder(ExtendedOrderCreateRequest request) {
        Order order = addOrder(request.order());
        for (OrderLineCreateRequest oRequest : request.lines()) {
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
        if (!request.status().equals(OrderStatus.CREATED)) {
            throw new InvalidRequestException("Order status must be CREATED");
        }
        Order order = getOrder(id);

        order.setLogicId(request.logicId());
        order.setDestinationLocation(getLocation(request.destinationLocationId()));
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
        if (order.getStatus() == OrderStatus.IN_PROGRESS || order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidRequestException("Order assignment is not allowed for IN_PROGRESS or COMPLETED orders");
        }

        assignTasks(order);

        assignOrderCascade(orderId, operatorId);
    }

    private void assignTasks(Order order) {
        for (OrderLine orderLine : order.getOrderLines()) {
            try {
                Task task = taskService.createTask(TaskType.PICKING_ORDER, orderLine.getRequestedQuantity(), orderLine.getProduct().getId());
                orderLine.setTask(task);
            } catch (Exception e) {
                orderLine.setStatus(Status.CANCELED);
            }
        }
        orderLineRepository.saveAllAndFlush(order.getOrderLines());
    }

    private void assignOrderCascade(Long orderId, Long operatorId) {
        int updated = orderRepository.updateStatus(orderId, OrderStatus.ASSIGNED);
        if (updated == 0) {
            throw new OrderNotFoundException(orderId);
        }
        log.info("Updated order with id {}", orderId);

        int tasksUpdated = taskRepository.updateOperatorByOrderId(orderId, operatorId);
        log.info("Updated {} tasks for order {}", tasksUpdated, orderId);

        int allocationsUpdated = allocationRepository.updateStatusByOrderId(orderId, Status.ASSIGNED);
        log.info("Updated {} allocations for order {}", allocationsUpdated, orderId);

        int orderLinesUpdated = orderLineRepository.updateStatusByOrderId(orderId, Status.ASSIGNED);
        log.info("Updated {} order lines for order {}", orderLinesUpdated, orderId);
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new LocationNotFoundException(locationId));
    }

    public ExtendedOrderResponse getExtendedOrderById(Long orderId) {
        return extendedOrderMapper.toResponse(getOrder(orderId));
    }

    public List<OrderResponse> searchOrders(OrderSearchRequest request) {
        return orderRepository.filter(
                request.logicId(),
                request.destinationLocationId(),
                request.status(),
                request.createdAt(),
                request.updatedAt()
            ).stream()
            .map(orderMapper::toResponse)
            .toList();
    }

    public List<ExtendedOrderResponse> getAllExtendedOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
            .map(extendedOrderMapper::toResponse)
            .toList();
    }

    @Transactional
    public void importOrdersFromFile(MultipartFile file) {
        List<ExtendedOrderCreateRequest> orders = importService.importData(file, ExtendedOrderInfo.class);
        try {
            orders.stream()
                .collect(Collectors.groupingBy(
                    ExtendedOrderCreateRequest::order
                ))
                .values()
                .stream()
                .map(group -> new ExtendedOrderCreateRequest(
                    group.getFirst().order(),
                    group.stream()
                        .map(ExtendedOrderCreateRequest::lines)
                        .flatMap(List::stream)
                        .toList()
                ))
                .forEach(this::addExtendedOrder);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("The imported file contains invalid order data.");
        }
    }
}
