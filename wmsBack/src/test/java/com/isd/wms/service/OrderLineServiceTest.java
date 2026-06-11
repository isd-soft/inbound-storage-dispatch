package com.isd.wms.service;

import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.dto.order_line.OrderLineUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.OrderLineNotFoundException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.TaskNotFoundException;
import com.isd.wms.mapper.OrderLineMapper;
import com.isd.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderLineServiceTest {

    private OrderRepository orderRepository;
    private OrderLineRepository orderLineRepository;
    private ProductRepository productRepository;
    private TaskRepository taskRepository;
    private OrderLineMapper orderLineMapper;
    private TaskService taskService;
    private OrderLineService orderLineService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderLineRepository = mock(OrderLineRepository.class);
        productRepository = mock(ProductRepository.class);
        taskRepository = mock(TaskRepository.class);
        orderLineMapper = mock(OrderLineMapper.class);
        taskService = mock(TaskService.class);
        orderLineService = new OrderLineService(orderRepository, orderLineRepository, orderLineMapper, productRepository, taskRepository, taskService);
    }

    private Order orderWithId(Long id) {
        Order order = new Order("LOGIC-00" + id);
        order.setId(id);
        return order;
    }

    private Task taskWithId(Long id) {
        Task task = new Task();
        task.setId(id);
        return task;
    }

    private Product productWithId(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    @Test
    void addOrderLine_validRequest_savesOrderLine() {
        Order order = orderWithId(1L);
        Product product = productWithId(1L);
        Task task = taskWithId(1L);
        OrderLineCreateRequest request = new OrderLineCreateRequest(1L, 1L, 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(taskService.createTask(TaskType.PICKING_ORDER, 10, 1L)).thenReturn(task);
        when(orderLineRepository.save(any(OrderLine.class))).thenAnswer(inv -> inv.getArgument(0));

        orderLineService.addOrderLine(order, request);

        verify(orderLineRepository).save(any(OrderLine.class));
    }

    @Test
    void addOrderLine_productNotFound_throwsProductNotFoundException() {
        Order order = orderWithId(1L);
        OrderLineCreateRequest request = new OrderLineCreateRequest(1L, 99L, 10);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderLineService.addOrderLine(order, request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(orderLineRepository, never()).save(any());
    }

    @Test
    void updateOrderLine_validRequest_returnsUpdatedResponse() {
        OrderLineUpdateRequest request = new OrderLineUpdateRequest(1L, 1L, 1L, 20, Status.IN_PROGRESS);
        Order order = orderWithId(1L);
        Product product = productWithId(1L);
        Task task = taskWithId(1L);
        OrderLine orderLine = new OrderLine(order, task, product, 10);
        orderLine.setId(1L);
        OrderLineResponse response = new OrderLineResponse(1L, 1L, 1L, 1L, 20, Status.IN_PROGRESS, null, null);

        when(orderLineRepository.findById(1L)).thenReturn(Optional.of(orderLine));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderLineRepository.save(orderLine)).thenReturn(orderLine);
        when(orderLineMapper.toResponse(orderLine)).thenReturn(response);

        OrderLineResponse result = orderLineService.updateOrderLine(1L, request);

        assertThat(result.requestedQuantity()).isEqualTo(20);
        assertThat(result.status()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void updateOrderLine_orderLineNotFound_throwsOrderLineNotFoundException() {
        OrderLineUpdateRequest request = new OrderLineUpdateRequest(1L, 1L, 1L, 20, Status.IN_PROGRESS);
        when(orderLineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderLineService.updateOrderLine(99L, request))
                .isInstanceOf(OrderLineNotFoundException.class);
    }

    @Test
    void updateOrderLine_orderNotFound_throwsOrderNotFoundException() {
        OrderLineUpdateRequest request = new OrderLineUpdateRequest(99L, 1L, 1L, 20, Status.IN_PROGRESS);
        OrderLine orderLine = new OrderLine();
        orderLine.setId(1L);
        when(orderLineRepository.findById(1L)).thenReturn(Optional.of(orderLine));
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderLineService.updateOrderLine(1L, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateOrderLine_taskNotFound_throwsTaskNotFoundException() {
        OrderLineUpdateRequest request = new OrderLineUpdateRequest(1L, 99L, 1L, 20, Status.IN_PROGRESS);
        OrderLine orderLine = new OrderLine();
        orderLine.setId(1L);
        when(orderLineRepository.findById(1L)).thenReturn(Optional.of(orderLine));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderWithId(1L)));
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderLineService.updateOrderLine(1L, request))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void updateOrderLine_productNotFound_throwsProductNotFoundException() {
        OrderLineUpdateRequest request = new OrderLineUpdateRequest(1L, 1L, 99L, 20, Status.IN_PROGRESS);
        OrderLine orderLine = new OrderLine();
        orderLine.setId(1L);
        when(orderLineRepository.findById(1L)).thenReturn(Optional.of(orderLine));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderWithId(1L)));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskWithId(1L)));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderLineService.updateOrderLine(1L, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteOrderLine_callsRepository() {
        doNothing().when(orderLineRepository).deleteById(1L);

        orderLineService.deleteOrderLine(1L);

        verify(orderLineRepository, times(1)).deleteById(1L);
    }

    @Test
    void getAll_returnsMappedList() {
        Order order = orderWithId(1L);
        Product product = productWithId(1L);
        Task task = taskWithId(1L);
        OrderLine orderLine = new OrderLine(order, task, product, 10);
        orderLine.setId(1L);
        OrderLineResponse response = new OrderLineResponse(1L, 1L, 1L, 1L, 10, Status.CREATED, null, null);
        when(orderLineRepository.findAll()).thenReturn(List.of(orderLine));
        when(orderLineMapper.toResponse(orderLine)).thenReturn(response);

        assertThat(orderLineService.getAll()).hasSize(1);
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        when(orderLineRepository.findAll()).thenReturn(List.of());

        assertThat(orderLineService.getAll()).isEmpty();
    }

    @Test
    void getOrderLineById_existingId_returnsResponse() {
        Order order = orderWithId(1L);
        Product product = productWithId(1L);
        Task task = taskWithId(1L);
        OrderLine orderLine = new OrderLine(order, task, product, 10);
        orderLine.setId(1L);
        OrderLineResponse response = new OrderLineResponse(1L, 1L, 1L, 1L, 10, Status.CREATED, null, null);
        when(orderLineRepository.findById(1L)).thenReturn(Optional.of(orderLine));
        when(orderLineMapper.toResponse(orderLine)).thenReturn(response);

        assertThat(orderLineService.getOrderLineById(1L).orderLineId()).isEqualTo(1L);
    }

    @Test
    void getOrderLineById_notFound_throwsOrderLineNotFoundException() {
        when(orderLineRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderLineService.getOrderLineById(99L))
                .isInstanceOf(OrderLineNotFoundException.class);
    }
}
