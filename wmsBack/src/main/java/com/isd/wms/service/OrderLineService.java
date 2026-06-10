package com.isd.wms.service;

import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.*;
import com.isd.wms.mapper.OrderLineMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderLineService {
    private final LocationRepository locationRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;
    private final ProductRepository productRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final SecurityFacade securityFacade;

    @Transactional
    public OrderLineResponse createOrderLine(OrderLineCreateRequest request) {
        Order order = getOrder(request.orderId());
        User supervisor = getUser(securityFacade.getCurrentUsername());

        Task task = new Task();
        task.setSupervisor(supervisor);
        task.setTaskType(TaskType.REPLENISHMENT);
        task.setRequestedQuantity(request.requestedQuantity());
        task.setStatus(TaskStatus.CREATED);

        task = taskRepository.save(task);

        workflowService.generateProcessesForTask(task, request.productId(), request.requestedQuantity());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        OrderLine orderLine = new OrderLine();
        orderLine.setOrder(order);
        orderLine.setTask(task);
        orderLine.setProduct(product);
        orderLine.setRequestedQuantity(request.requestedQuantity());
        orderLine.setDestinationLocation(destinationLocation);
        orderLine.setStatus(OrderStatus.CREATED);

        return orderLineMapper.toResponse(orderLineRepository.save(orderLine));
    }

    @Transactional
    public OrderLineResponse updateOrderLine(Long id, OrderLineUpdateRequest request) {

        OrderLine orderLine = getOrderLine(id);
        Order order = getOrder(request.orderId());
        Product product = getProduct(request.productId());
        Task task = getTask(request.taskId());
        Location location = getLocation(request.destinationLocationId());

        orderLine.setOrder(order);
        orderLine.setTask(task);
        orderLine.setProduct(product);
        orderLine.setRequestedQuantity(request.requestedQuantity());
        orderLine.setStatus(request.status());
        orderLine.setDestinationLocation(location);

        return orderLineMapper.toResponse(orderLineRepository.save(orderLine));
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

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
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