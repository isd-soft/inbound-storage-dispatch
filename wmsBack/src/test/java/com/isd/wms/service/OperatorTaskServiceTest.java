package com.isd.wms.service;

import com.isd.wms.dto.operator.OperatorTaskSummaryResponse;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Role;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.AllocationRepository  ;
import com.isd.wms.service.validation.SecurityFacade;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatorTaskServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private com.isd.wms.repository.StockRepository stockRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private AllocationRepository  allocationRepository ;

    @Mock
    private com.isd.wms.repository.ReplenishmentRepository replenishmentRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private WorkflowService workflowService;

    @Spy
    private PickingFlowService pickingFlowService;

    @InjectMocks
    private AllocationExecutionService allocationExecutionService;

    private User operator;
    private Order order;
    private OrderLine orderLine;
    private Allocation allocation;
    private Allocation secondAllocation;

    @BeforeEach
    void setUp() {
        operator = new User("operator", "operator@example.com", "pass", Role.ROLE_OPERATOR, false, null, null);
        ReflectionTestUtils.setField(operator, "id", 10L);

        Location destination = new Location("Dispatch", "DISP-01", null, null, true);
        ReflectionTestUtils.setField(destination, "id", 100L);

        Product product = new Product("Widget", "WGT-01", null, null);
        ReflectionTestUtils.setField(product, "id", 200L);

        order = new Order("ORD-1", destination);
        ReflectionTestUtils.setField(order, "id", 300L);
        ReflectionTestUtils.setField(order, "status", OrderStatus.ASSIGNED);

        Task task = new Task(null, TaskType.PICKING_ORDER, 5);
        ReflectionTestUtils.setField(task, "id", 400L);
        task.setOperator(operator);

        orderLine = new OrderLine(order, task, product, 5);
        ReflectionTestUtils.setField(orderLine, "id", 500L);
        ReflectionTestUtils.setField(orderLine, "status", Status.ASSIGNED);

        Location source = new Location("Pick", "PICK-01", null, null, true);
        ReflectionTestUtils.setField(source, "id", 101L);
        Stock stock = new Stock(product, source);
        ReflectionTestUtils.setField(stock, "id", 600L);
        stock.setQuantity(20);
        stock.setReservedQuantity(5);

        allocation = new Allocation(task, stock, 5, Status.ASSIGNED);
        ReflectionTestUtils.setField(allocation, "id", 700L);
        ReflectionTestUtils.setField(allocation, "createdAt", LocalDateTime.of(2026, 6, 15, 9, 0));

        Task secondTask = new Task(null, TaskType.PICKING_ORDER, 3);
        ReflectionTestUtils.setField(secondTask, "id", 401L);
        secondTask.setOperator(operator);

        OrderLine secondOrderLine = new OrderLine(order, secondTask, product, 3);
        ReflectionTestUtils.setField(secondOrderLine, "id", 501L);
        ReflectionTestUtils.setField(secondOrderLine, "status", Status.ASSIGNED);

        Stock secondStock = new Stock(product, source);
        ReflectionTestUtils.setField(secondStock, "id", 601L);
        secondStock.setQuantity(20);
        secondStock.setReservedQuantity(3);

        secondAllocation = new Allocation(secondTask, secondStock, 3, Status.ASSIGNED);
        ReflectionTestUtils.setField(secondAllocation, "id", 701L);
        ReflectionTestUtils.setField(secondAllocation, "createdAt", LocalDateTime.of(2026, 6, 15, 9, 2));

        lenient().when(securityFacade.getCurrentUser()).thenReturn(operator);
        lenient().when(securityFacade.getCurrentUsername()).thenReturn("operator");
        lenient().when(orderRepository.findOldestOrderAssignedToOperator(10L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.findOldestPickedOrderAssignedToOperator(10L)).thenReturn(Optional.empty());
        lenient().when(orderLineRepository.findAllByOrderId(300L)).thenReturn(List.of(orderLine));
    }

    @Test
    void getCurrentSummaryReturnsAssignedPickingTask() {
        when(allocationRepository.findAllByOrder(order)).thenReturn(List.of(allocation));
        when(allocationRepository.findByOperatorUsernameAndStatuses("operator", List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(allocation));
        when(orderLineRepository.findByTaskId(400L)).thenReturn(Optional.of(orderLine));

        Optional<OperatorTaskSummaryResponse> summary = allocationExecutionService.getCurrentSummary();

        assertThat(summary).isPresent();
        assertThat(summary.get().taskType()).isEqualTo(TaskType.PICKING_ORDER.name());
        assertThat(summary.get().currentAllocation()).isNotNull();
        assertThat(summary.get().currentAllocation().sourceLocationBarcode()).isEqualTo("PICK-01");
        assertThat(summary.get().orderLines()).hasSize(1);
        assertThat(summary.get().allocations()).hasSize(1);
        assertThat(summary.get().readyForCompletion()).isFalse();
    }

    @Test
    void startCurrentTaskMovesAssignedAllocationAndOrderToInProgress() {
        when(allocationRepository.findAllByOrder(order)).thenReturn(List.of(allocation, secondAllocation));
        when(allocationRepository.findByOperatorUsernameAndStatuses("operator", List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(allocation, secondAllocation));
        when(orderLineRepository.findByTaskId(400L)).thenReturn(Optional.of(orderLine));

        OperatorTaskSummaryResponse response = allocationExecutionService.startCurrentTask();

        assertThat(allocation.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(orderLine.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(response.currentAllocation()).isNotNull();
        assertThat(response.currentAllocation().status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.allocations()).hasSize(2);
        verify(orderRepository).save(order);
    }

    @Test
    void getCurrentSummaryMarksOrderReadyForCompletionWhenPicked() {
        when(allocationRepository.findAllByOrder(order)).thenReturn(List.of(allocation));
        when(orderRepository.findOldestPickedOrderAssignedToOperator(10L)).thenReturn(Optional.of(order));

        ReflectionTestUtils.setField(order, "status", OrderStatus.PICKED);
        ReflectionTestUtils.setField(orderLine, "status", Status.COMPLETED);
        ReflectionTestUtils.setField(allocation, "status", Status.COMPLETED);
        ReflectionTestUtils.setField(allocation, "pickedQuantity", 5);

        Optional<OperatorTaskSummaryResponse> summary = allocationExecutionService.getCurrentSummary();

        assertThat(summary).isPresent();
        assertThat(summary.get().readyForCompletion()).isTrue();
        assertThat(summary.get().currentAllocation()).isNull();
        assertThat(summary.get().orderLines().getFirst().pickedQuantity()).isEqualTo(5);
    }

    @Test
    void completeCurrentOrderFinalizesPickedOrder() {
        when(allocationRepository.findAllByOrder(order)).thenReturn(List.of(allocation));
        when(orderRepository.findOldestPickedOrderAssignedToOperator(10L)).thenReturn(Optional.of(order));

        ReflectionTestUtils.setField(order, "status", OrderStatus.PICKED);
        ReflectionTestUtils.setField(orderLine, "status", Status.COMPLETED);
        ReflectionTestUtils.setField(allocation, "status", Status.COMPLETED);

        allocationExecutionService.completeCurrentOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(orderRepository).save(order);
    }

    @Test
    void completeCurrentOrderRejectsIncompleteOrder() {
        when(orderRepository.findOldestPickedOrderAssignedToOperator(10L)).thenReturn(Optional.of(order));

        ReflectionTestUtils.setField(order, "status", OrderStatus.PICKED);
        ReflectionTestUtils.setField(orderLine, "status", Status.ASSIGNED);
        ReflectionTestUtils.setField(allocation, "status", Status.COMPLETED);

        assertThatThrownBy(() -> allocationExecutionService.completeCurrentOrder())
            .hasMessage("All order lines must be completed before final confirmation");
    }

    @Test
    void getCurrentSummaryKeepsOperatorInSameLocationUntilLocationIsCompleted() {
        Location otherLocation = new Location("Pick B", "PICK-02", null, null, true);
        ReflectionTestUtils.setField(otherLocation, "id", 102L);

        Product otherProduct = new Product("Widget B", "WGT-02", null, null);
        ReflectionTestUtils.setField(otherProduct, "id", 201L);

        Task thirdTask = new Task(null, TaskType.PICKING_ORDER, 2);
        ReflectionTestUtils.setField(thirdTask, "id", 402L);
        thirdTask.setOperator(operator);

        OrderLine thirdOrderLine = new OrderLine(order, thirdTask, otherProduct, 2);
        ReflectionTestUtils.setField(thirdOrderLine, "id", 502L);
        ReflectionTestUtils.setField(thirdOrderLine, "status", Status.ASSIGNED);

        Stock thirdStock = new Stock(otherProduct, otherLocation);
        ReflectionTestUtils.setField(thirdStock, "id", 602L);
        thirdStock.setQuantity(20);
        thirdStock.setReservedQuantity(2);

        Allocation thirdAllocation = new Allocation(thirdTask, thirdStock, 2, Status.ASSIGNED);
        ReflectionTestUtils.setField(thirdAllocation, "id", 702L);
        ReflectionTestUtils.setField(thirdAllocation, "createdAt", LocalDateTime.of(2026, 6, 15, 9, 1));

        ReflectionTestUtils.setField(allocation, "status", Status.COMPLETED);
        ReflectionTestUtils.setField(allocation, "pickedQuantity", 5);
        when(allocationRepository.findAllByOrder(order)).thenReturn(List.of(allocation, thirdAllocation, secondAllocation));
        when(allocationRepository.findByOperatorUsernameAndStatuses("operator", List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(thirdAllocation, secondAllocation));
        when(orderLineRepository.findByTaskId(402L)).thenReturn(Optional.of(thirdOrderLine));

        Optional<OperatorTaskSummaryResponse> summary = allocationExecutionService.getCurrentSummary();

        assertThat(summary).isPresent();
        assertThat(summary.get().currentAllocation()).isNotNull();
        assertThat(summary.get().currentAllocation().allocationId()).isEqualTo(701L);
        assertThat(summary.get().currentAllocation().sourceLocationBarcode()).isEqualTo("PICK-01");
    }

    @Test
    void getCurrentSummaryReturnsReplenishmentDestinationForAssignedOperator() {
        Location replenishmentDestination = new Location("Pick Face", "PICK-FACE-01", null, null, true);
        ReflectionTestUtils.setField(replenishmentDestination, "id", 103L);

        Task replenishmentTask = new Task(null, TaskType.REPLENISHMENT, 4);
        ReflectionTestUtils.setField(replenishmentTask, "id", 450L);
        replenishmentTask.setOperator(operator);

        Product replenishmentProduct = new Product("Refill Widget", "REF-01", null, null);
        ReflectionTestUtils.setField(replenishmentProduct, "id", 202L);

        Location source = new Location("Bulk", "BULK-01", null, null, true);
        ReflectionTestUtils.setField(source, "id", 104L);
        Stock replenishmentStock = new Stock(replenishmentProduct, source);
        ReflectionTestUtils.setField(replenishmentStock, "id", 603L);
        replenishmentStock.setQuantity(10);
        replenishmentStock.setReservedQuantity(4);

        Allocation replenishmentAllocation = new Allocation(replenishmentTask, replenishmentStock, 4, Status.ASSIGNED);
        ReflectionTestUtils.setField(replenishmentAllocation, "id", 703L);
        ReflectionTestUtils.setField(replenishmentAllocation, "createdAt", LocalDateTime.of(2026, 6, 15, 10, 0));

        Replenishment replenishment = new Replenishment(replenishmentTask, replenishmentProduct, 4, replenishmentDestination);
        ReflectionTestUtils.setField(replenishment, "id", 800L);

        when(allocationRepository.findByOperatorUsernameAndStatuses("operator", List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(replenishmentAllocation));
        when(allocationRepository.findAllByTaskId(450L)).thenReturn(List.of(replenishmentAllocation));
        when(replenishmentRepository.findByTaskId(450L)).thenReturn(Optional.of(replenishment));

        Optional<OperatorTaskSummaryResponse> summary = allocationExecutionService.getCurrentSummary();

        assertThat(summary).isPresent();
        assertThat(summary.get().taskType()).isEqualTo(TaskType.REPLENISHMENT.name());
        assertThat(summary.get().destinationLocationBarcode()).isEqualTo("PICK-FACE-01");
        assertThat(summary.get().currentAllocation().sourceLocationBarcode()).isEqualTo("BULK-01");
        assertThat(summary.get().currentAllocation().destinationLocationBarcode()).isEqualTo("PICK-FACE-01");
        assertThat(summary.get().totalAllocations()).isEqualTo(1);
    }

    @Test
    void startCurrentTaskMovesAssignedReplenishmentToInProgress() {
        Location replenishmentDestination = new Location("Pick Face", "PICK-FACE-01", null, null, true);
        ReflectionTestUtils.setField(replenishmentDestination, "id", 103L);

        Task replenishmentTask = new Task(null, TaskType.REPLENISHMENT, 4);
        ReflectionTestUtils.setField(replenishmentTask, "id", 451L);
        replenishmentTask.setOperator(operator);

        Product replenishmentProduct = new Product("Refill Widget", "REF-01", null, null);
        ReflectionTestUtils.setField(replenishmentProduct, "id", 202L);

        Location source = new Location("Bulk", "BULK-01", null, null, true);
        ReflectionTestUtils.setField(source, "id", 104L);
        Stock replenishmentStock = new Stock(replenishmentProduct, source);
        ReflectionTestUtils.setField(replenishmentStock, "id", 603L);
        replenishmentStock.setQuantity(10);
        replenishmentStock.setReservedQuantity(4);

        Allocation replenishmentAllocation = new Allocation(replenishmentTask, replenishmentStock, 4, Status.ASSIGNED);
        ReflectionTestUtils.setField(replenishmentAllocation, "id", 704L);
        ReflectionTestUtils.setField(replenishmentAllocation, "createdAt", LocalDateTime.of(2026, 6, 15, 10, 0));

        Replenishment replenishment = new Replenishment(replenishmentTask, replenishmentProduct, 4, replenishmentDestination);
        ReflectionTestUtils.setField(replenishment, "id", 801L);
        ReflectionTestUtils.setField(replenishment, "status", Status.ASSIGNED);

        when(allocationRepository.findByOperatorUsernameAndStatuses("operator", List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(replenishmentAllocation));
        when(allocationRepository.findAllByTaskId(451L)).thenReturn(List.of(replenishmentAllocation));
        when(replenishmentRepository.findByTaskId(451L)).thenReturn(Optional.of(replenishment));

        OperatorTaskSummaryResponse response = allocationExecutionService.startCurrentTask();

        assertThat(replenishmentAllocation.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(replenishment.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.currentAllocation().status()).isEqualTo(Status.IN_PROGRESS);
        assertThat(response.destinationLocationBarcode()).isEqualTo("PICK-FACE-01");
    }

    @Test
    void getCurrentSummaryKeepsGlobalFifoAcrossReplenishmentAndOrders() {
        Location replenishmentDestination = new Location("Pick Face", "PICK-FACE-01", null, null, true);
        ReflectionTestUtils.setField(replenishmentDestination, "id", 103L);

        Task replenishmentTask = new Task(null, TaskType.REPLENISHMENT, 4);
        ReflectionTestUtils.setField(replenishmentTask, "id", 451L);
        replenishmentTask.setOperator(operator);

        Product replenishmentProduct = new Product("Refill Widget", "REF-01", null, null);
        ReflectionTestUtils.setField(replenishmentProduct, "id", 202L);

        Location bulkLocation = new Location("Bulk", "BULK-01", null, null, true);
        ReflectionTestUtils.setField(bulkLocation, "id", 104L);
        Stock replenishmentStock = new Stock(replenishmentProduct, bulkLocation);
        ReflectionTestUtils.setField(replenishmentStock, "id", 603L);
        replenishmentStock.setQuantity(10);
        replenishmentStock.setReservedQuantity(4);

        Allocation replenishmentAllocation = new Allocation(replenishmentTask, replenishmentStock, 4, Status.ASSIGNED);
        ReflectionTestUtils.setField(replenishmentAllocation, "id", 704L);
        ReflectionTestUtils.setField(replenishmentAllocation, "createdAt", LocalDateTime.of(2026, 6, 15, 8, 55));

        Replenishment replenishment = new Replenishment(replenishmentTask, replenishmentProduct, 4, replenishmentDestination);
        ReflectionTestUtils.setField(replenishment, "id", 801L);

        when(allocationRepository.findByOperatorUsernameAndStatuses("operator", List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(replenishmentAllocation, allocation));
        when(allocationRepository.findAllByTaskId(451L)).thenReturn(List.of(replenishmentAllocation));
        when(replenishmentRepository.findByTaskId(451L)).thenReturn(Optional.of(replenishment));

        Optional<OperatorTaskSummaryResponse> summary = allocationExecutionService.getCurrentSummary();

        assertThat(summary).isPresent();
        assertThat(summary.get().taskType()).isEqualTo(TaskType.REPLENISHMENT.name());
        assertThat(summary.get().taskId()).isEqualTo(451L);
        assertThat(summary.get().currentAllocation().allocationId()).isEqualTo(704L);
    }
}
