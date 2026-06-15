package com.isd.wms.service.allocation;

import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.AllocationRepository  ;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickingAllocationCompletionStrategyTest {

    @Mock
    private AllocationRepository  allocationRepository ;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PickingAllocationCompletionStrategy strategy;

    @Test
    void handleMarksOrderPickedInsteadOfCompleted() {
        Location destination = new Location("Dispatch", "DISP-01", null, null, true);
        Product product = new Product("Widget", "WGT-01", null, null);
        Location source = new Location("Pick", "PICK-01", null, null, true);
        Stock stock = new Stock(product, source);

        Order order = new Order("ORD-1", destination);
        ReflectionTestUtils.setField(order, "status", OrderStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(order, "id", 1L);

        Task task = new Task(null, TaskType.PICKING_ORDER, 5);
        ReflectionTestUtils.setField(task, "id", 2L);

        OrderLine orderLine = new OrderLine(order, task, product, 5);
        ReflectionTestUtils.setField(orderLine, "status", Status.IN_PROGRESS);
        ReflectionTestUtils.setField(order, "orderLines", List.of(orderLine));

        Allocation allocation = new Allocation(task, stock, 5, Status.COMPLETED);
        ReflectionTestUtils.setField(allocation, "id", 3L);

        when(allocationRepository.findAllByTaskId(2L)).thenReturn(List.of(allocation));
        when(orderLineRepository.findByTaskId(2L)).thenReturn(Optional.of(orderLine));

        strategy.handle(allocation);

        assertThat(orderLine.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED);
        verify(orderRepository).save(order);
    }
}
