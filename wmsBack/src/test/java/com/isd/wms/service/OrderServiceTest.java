package com.isd.wms.service;

import com.isd.wms.dto.order.*;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.entity.Order;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.mapper.ExtendedOrderMapper;
import com.isd.wms.mapper.OrderMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.validation.SecurityFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private LocationRepository locationRepository;
    private OrderMapper orderMapper;
    private ExtendedOrderMapper extendedOrderMapper;
    private OrderLineService orderLineService;
    private OrderService orderService;
    private AllocationRepository  allocationRepository ;
    private TaskRepository taskRepository;
    private OrderLineRepository orderLineRepository;
    private SecurityFacade securityFacade;

    @BeforeEach
    void setUp() {
        extendedOrderMapper = mock(ExtendedOrderMapper.class);
        orderMapper = mock(OrderMapper.class);
        orderRepository = mock(OrderRepository.class);
        locationRepository = mock(LocationRepository.class);
        orderLineService = mock(OrderLineService.class);
        allocationRepository  = mock(AllocationRepository.class);
        taskRepository = mock(TaskRepository.class);
        orderLineRepository = mock(OrderLineRepository.class);
        securityFacade = mock(SecurityFacade.class);
        orderService = new OrderService(extendedOrderMapper, orderMapper, orderRepository, locationRepository, orderLineService, allocationRepository , taskRepository, orderLineRepository, securityFacade);
    }

    private Order orderWithId(Long id, String logicId) {
        Order order = new Order(logicId);
        order.setId(id);
        return order;
    }

    private OrderResponse sampleOrderResponse() {
        return new OrderResponse(1L, "LOGIC-001", 1L, OrderStatus.CREATED, null, null);
    }

    private ExtendedOrderResponse sampleExtendedOrderResponse() {
        return new ExtendedOrderResponse(sampleOrderResponse(), List.of());
    }

    @Test
    void addOrder_validRequest_savesAndReturnsOrder() {
        OrderCreateRequest request = new OrderCreateRequest("LOGIC-001", 1L);
        Order saved = orderWithId(1L, "LOGIC-001");
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(securityFacade.getCurrentUser()).thenReturn(new com.isd.wms.entity.User());

        Order result = orderService.addOrder(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLogicId()).isEqualTo("LOGIC-001");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void addExtendedOrder_withLines_savesOrderAndDelegatesLines() {
        OrderLineCreateRequest lineRequest = new OrderLineCreateRequest(null, 1L, 5);
        ExtendedOrderCreateRequest request = new ExtendedOrderCreateRequest(
                new OrderCreateRequest("LOGIC-001", 1L),
                List.of(lineRequest)
        );
        Order saved = orderWithId(1L, "LOGIC-001");

        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(sampleOrderResponse());
        when(securityFacade.getCurrentUser()).thenReturn(new com.isd.wms.entity.User());
        doNothing().when(orderLineService).addOrderLine(eq(saved), any(OrderLineCreateRequest.class));

        OrderResponse result = orderService.addExtendedOrder(request);

        assertThat(result.id()).isEqualTo(1L);
        verify(orderLineService, times(1)).addOrderLine(eq(saved), any(OrderLineCreateRequest.class));
    }

    @Test
    void addExtendedOrder_noLines_neverCallsOrderLineService() {
        ExtendedOrderCreateRequest request = new ExtendedOrderCreateRequest(
                new OrderCreateRequest("LOGIC-001", 1L),
                List.of()
        );
        Order saved = orderWithId(1L, "LOGIC-001");

        when(orderRepository.save(any(Order.class))).thenReturn(saved);
        when(orderMapper.toResponse(saved)).thenReturn(sampleOrderResponse());
        when(securityFacade.getCurrentUser()).thenReturn(new com.isd.wms.entity.User());

        orderService.addExtendedOrder(request);

        verify(orderLineService, never()).addOrderLine(any(), any());
    }

    @Test
    void updateOrder_statusIsCreated_updatesAndReturns() {
        OrderUpdateRequest request = new OrderUpdateRequest("LOGIC-002", 1L, OrderStatus.CREATED);
        Order existing = orderWithId(1L, "LOGIC-001");
        Order updated = orderWithId(1L, "LOGIC-002");
        OrderResponse response = new OrderResponse(1L, "LOGIC-002", 1L, OrderStatus.CREATED, null, null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(updated);
        when(orderMapper.toResponse(updated)).thenReturn(response);

        OrderResponse result = orderService.updateOrder(1L, request);

        assertThat(result.logicId()).isEqualTo("LOGIC-002");
    }

    @Test
    void updateOrder_statusNotCreated_throwsInvalidRequestException() {
        OrderUpdateRequest request = new OrderUpdateRequest("LOGIC-002", 1L, OrderStatus.IN_PROGRESS);

        assertThatThrownBy(() -> orderService.updateOrder(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("CREATED");

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void updateOrder_orderNotFound_throwsOrderNotFoundException() {
        OrderUpdateRequest request = new OrderUpdateRequest("LOGIC-002", 1L, OrderStatus.CREATED);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(99L, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deleteOrderById_callsRepository() {
        doNothing().when(orderRepository).deleteById(1L);

        orderService.deleteOrderById(1L);

        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void getAllOrders_returnsMappedList() {
        Order order = orderWithId(1L, "LOGIC-001");
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findAllByCreatedByUsername(any())).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(sampleOrderResponse());

        List<OrderResponse> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).logicId()).isEqualTo("LOGIC-001");
    }

    @Test
    void getAllOrders_empty_returnsEmptyList() {
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findAllByCreatedByUsername(any())).thenReturn(List.of());

        assertThat(orderService.getAllOrders()).isEmpty();
    }

    @Test
    void getOrderById_existingId_returnsResponse() {
        Order order = orderWithId(1L, "LOGIC-001");
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findByIdAndCreatedByUsername(eq(1L), any())).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(sampleOrderResponse());

        assertThat(orderService.getOrderById(1L).id()).isEqualTo(1L);
    }

    @Test
    void getOrderById_notFound_throwsOrderNotFoundException() {
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findByIdAndCreatedByUsername(eq(99L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getExtendedOrderById_existingId_returnsExtendedResponse() {
        Order order = orderWithId(1L, "LOGIC-001");
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findByIdAndCreatedByUsername(eq(1L), any())).thenReturn(Optional.of(order));
        when(extendedOrderMapper.toResponse(order)).thenReturn(sampleExtendedOrderResponse());

        assertThat(orderService.getExtendedOrderById(1L).order().id()).isEqualTo(1L);
    }

    @Test
    void getExtendedOrderById_notFound_throwsOrderNotFoundException() {
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findByIdAndCreatedByUsername(eq(99L), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getExtendedOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getAllExtendedOrders_returnsMappedList() {
        Order order = orderWithId(1L, "LOGIC-001");
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findAllByCreatedByUsername(any())).thenReturn(List.of(order));
        when(extendedOrderMapper.toResponse(order)).thenReturn(sampleExtendedOrderResponse());

        assertThat(orderService.getAllExtendedOrders()).hasSize(1);
    }

    @Test
    void assignOrder_completedOrder_throwsInvalidRequestException() {
        Order order = orderWithId(1L, "LOGIC-001");
        order.setStatus(OrderStatus.COMPLETED);
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.findByIdAndCreatedByUsername(eq(1L), any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.assignOrder(1L, 10L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not allowed");

        verify(orderRepository, never()).updateStatus(any(), any());
        verify(taskRepository, never()).updateOperatorByOrderId(any(), any());
    }

    @Test
    void searchOrders_withFilters_returnsMappedList() {
        OrderSearchRequest request = new OrderSearchRequest(null, "LOGIC-001", null, OrderStatus.CREATED, null, null);
        Order order = orderWithId(1L, "LOGIC-001");
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.filter(any(), eq("LOGIC-001"), any(), eq(OrderStatus.CREATED), any(), any())).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(sampleOrderResponse());

        assertThat(orderService.searchOrders(request)).hasSize(1);
    }

    @Test
    void searchOrders_noMatch_returnsEmptyList() {
        OrderSearchRequest request = new OrderSearchRequest(null, "NONEXISTENT", null, null, null, null);
        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(orderRepository.filter(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        assertThat(orderService.searchOrders(request)).isEmpty();
    }
}
