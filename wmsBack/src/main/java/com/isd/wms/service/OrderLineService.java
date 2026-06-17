package com.isd.wms.service;

import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Task;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class OrderLineService {
    private final OrderRepository orderRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderLineMapper orderLineMapper;
    private final ProductRepository productRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void addOrderLine(Order order, OrderLineCreateRequest request) {
        Product product = getProduct(request.productId());
        OrderLine orderLine = new OrderLine(order, product, request.requestedQuantity());
        orderLineRepository.save(orderLine);
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

    private Task getTask(Long taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
    }
}
