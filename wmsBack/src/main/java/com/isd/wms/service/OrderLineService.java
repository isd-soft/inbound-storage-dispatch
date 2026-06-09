package com.isd.wms.service;

import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.*;
import com.isd.wms.mapper.OrderLineMapper;
import com.isd.wms.repository.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OrderLineService {
    private final LocationRepository locationRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;
    private final ProductRepository productRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;

    @Transactional
    public OrderLineResponse addOrderLine(OrderLineCreateRequest request) {
        Order order = getOrder(request.orderId());


        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        Task task = taskService.createTask(TaskType.PICKING_ORDER, request.requestedQuantity(), request.productId());

        OrderLine orderLine = new OrderLine(order, task, product, request.requestedQuantity(), OrderStatus.CREATED, destinationLocation);

        return orderLineMapper.toResponse(orderLineRepository.save(orderLine));
    }

    @Transactional
    public OrderLineResponse updateOrderLine(Long id, OrderLineUpdateRequest request) {
        OrderLine orderLine = getOrderLine(id);

        updateOrderLineFields(orderLine, request);

        return orderLineMapper.toResponse(orderLineRepository.save(orderLine));
    }

    private void updateOrderLineFields(OrderLine orderLine, OrderLineUpdateRequest request) {
        orderLine.setOrder(getOrder(request.orderId()));
        orderLine.setTask(getTask(request.taskId()));
        orderLine.setProduct(getProduct(request.productId()));
        orderLine.setDestinationLocation(getLocation(request.destinationLocationId()));
        orderLine.setRequestedQuantity(request.requestedQuantity());
        orderLine.setStatus(request.status());
    }

    public void deleteOrderLine(Long orderLineId) {
        orderLineRepository.deleteById(orderLineId);
    }

    public List<OrderLineResponse> getAll() {
        return orderLineRepository.findAll().stream()
                .map(orderLineMapper::toResponse)
                .toList();
    }

    public List<OrderLine> getAllOrderLinesByOrderId(Long orderId) {
        return orderLineRepository.findAllByOrderId(orderId);
    }

    private Order getOrder(@NonNull Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public OrderLine getOrderLine(@NonNull Long orderLineId) {
        return orderLineRepository.findById(orderLineId)
                .orElseThrow(() -> new OrderLineNotFoundException(orderLineId));
    }

    public OrderLineResponse getOrderLineById(@NonNull Long orderLineId) {
        return orderLineMapper.toResponse(getOrderLine(orderLineId));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }

    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
