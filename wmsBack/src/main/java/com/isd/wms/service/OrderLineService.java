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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Transactional
    public OrderLineResponse addOrderLine(OrderLineCreateRequest request) {
        validateOrderLineRequest(
                request.orderId(),
                request.taskId(),
                request.productId(),
                request.requestedQuantity(),
                request.destinationLocationId()
        );
        Order order = getOrder(request.orderId());

        User supervisor = getUser(getCurrentUsername());
        Task task = Task.builder()
                .supervisor(supervisor)
                .taskType(TaskType.REPLENISHMENT)
                .requestedQuantity(request.requestedQuantity())
                .status(TaskStatus.CREATED)
                .build();
        task = taskRepository.save(task);

        workflowService.generateProcessesForTask(task, request.productId(), request.requestedQuantity());

        Product product = getProduct(request.productId());
        Location destinationLocation = getLocation(request.destinationLocationId());

        OrderLine orderLine = OrderLine.builder()
                .order(order)
                .task(task)
                .product(product)
                .requestedQuantity(request.requestedQuantity())
                .destinationLocation(destinationLocation)
                .status(OrderStatus.CREATED)
                .build();

        return orderLineMapper.toResponse(orderLineRepository.save(orderLine));
    }

    @Transactional
    public OrderLineResponse updateOrderLine(Long id, OrderLineUpdateRequest request) {
        validateOrderLineRequest(
                id,
                request.orderId(),
                request.taskId(),
                request.productId(),
                request.requestedQuantity(),
                request.status(),
                request.destinationLocationId()
        );

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

    public boolean deleteOrderLine(Long orderLineId) {
        if (orderLineRepository.existsById(orderLineId)) {
            orderLineRepository.deleteById(orderLineId);
            return true;
        }
        return false;
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

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
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

    private void validateOrderLineRequest(
            @NonNull Long orderId,
            @NonNull Long taskId,
            @NonNull Long productId,
            @NonNull Integer requestedQuantity,
            @NonNull Long destinationLocationId
    ) {
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException("Replenishment requested quantity cannot be nonpositive");
        }
    }

    private void validateOrderLineRequest(
            @NonNull Long orderLineId,
            @NonNull Long orderId,
            @NonNull Long taskId,
            @NonNull Long productId,
            @NonNull Integer requestedQuantity,
            @NonNull OrderStatus status,
            @NonNull Long destinationLocationId
    ) {
        if (requestedQuantity <= 0) {
            throw new InvalidRequestException("Replenishment requested quantity cannot be nonpositive");
        }
    }
}
