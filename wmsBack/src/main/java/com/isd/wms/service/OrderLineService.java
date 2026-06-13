package com.isd.wms.service;

import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.OrderLineNotFoundException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.TaskNotFoundException;
import com.isd.wms.mapper.OrderLineMapper;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.TaskRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OrderLineService {
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;
    private final ProductRepository productRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;

    @Transactional
    public void addOrderLine(Order order, OrderLineCreateRequest request) {
        log.info("Adding new OrderLine to Order ID: {}. Product ID: {}, Requested Qty: {}",
            order.getId(), request.productId(), request.requestedQuantity());

        Product product = getProduct(request.productId());

        log.debug("Triggering automatic task creation of type PICKING_ORDER for product '{}'", product.getName());
        Task task = taskService.createTask(TaskType.PICKING_ORDER, request.requestedQuantity(), request.productId());
        log.info("Successfully generated Task ID: {} for OrderLine", task.getId());

        OrderLine orderLine = new OrderLine(order, task, product, request.requestedQuantity());
        OrderLine savedLine = orderLineRepository.save(orderLine);

        log.info("OrderLine ID: {} successfully attached to Order ID: {}", savedLine.getId(), order.getId());
    }

    @Transactional
    public OrderLineResponse updateOrderLine(Long id, OrderLineUpdateRequest request) {
        log.info("Updating OrderLine ID: {}. New Target Order ID: {}, Task ID: {}, Product ID: {}, Qty: {}, Status: {}",
            id, request.orderId(), request.taskId(), request.productId(), request.requestedQuantity(), request.status());

        OrderLine orderLine = getOrderLine(id);
        updateOrderLineFields(orderLine, request);

        OrderLine updatedLine = orderLineRepository.save(orderLine);
        log.info("OrderLine ID: {} successfully updated", updatedLine.getId());

        return orderLineMapper.toResponse(updatedLine);
    }

    private void updateOrderLineFields(OrderLine orderLine, OrderLineUpdateRequest request) {
        orderLine.setOrder(getOrder(request.orderId()));
        orderLine.setTask(getTask(request.taskId()));
        orderLine.setProduct(getProduct(request.productId()));
        orderLine.setRequestedQuantity(request.requestedQuantity());
        orderLine.setStatus(request.status());
    }

    public void deleteOrderLine(Long orderLineId) {
        log.warn("Hard-delete requested for OrderLine ID: {}", orderLineId);
        orderLineRepository.deleteById(orderLineId);
        log.warn("OrderLine ID: {} was permanently deleted from the database", orderLineId);
    }

    public List<OrderLineResponse> getAll() {
        return orderLineRepository.findAll().stream()
                .map(orderLineMapper::toResponse)
                .toList();
    }

    private Order getOrder(@NonNull Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> {
                log.warn("Order lookup failed. Order ID: {} not found", orderId);
                return new OrderNotFoundException(orderId);
            });
    }

    public OrderLine getOrderLine(@NonNull Long orderLineId) {
        return orderLineRepository.findById(orderLineId)
            .orElseThrow(() -> {
                log.warn("OrderLine lookup failed. OrderLine ID: {} not found", orderLineId);
                return new OrderLineNotFoundException(orderLineId);
            });
    }

    public OrderLineResponse getOrderLineById(@NonNull Long orderLineId) {
        return orderLineMapper.toResponse(getOrderLine(orderLineId));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> {
                log.warn("Product lookup failed inside OrderLine context. Product ID: {} not found", productId);
                return new ProductNotFoundException(productId);
            });
    }

    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> {
                log.warn("Task lookup failed inside OrderLine context. Task ID: {} not found", taskId);
                return new TaskNotFoundException(taskId);
            });
    }
}
