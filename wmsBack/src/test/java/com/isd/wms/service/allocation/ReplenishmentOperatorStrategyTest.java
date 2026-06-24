package com.isd.wms.service.allocation;

import com.isd.wms.dto.allocation.AllocationCompletionResponse;
import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.enums.Zone;
import com.isd.wms.mapper.OperatorSummaryMapper;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TransportUnitRepository;
import com.isd.wms.service.InventoryService;
import com.isd.wms.service.PickingFlowService;
import com.isd.wms.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplenishmentOperatorStrategyTest {

    @Mock private AllocationRepository allocationRepository;
    @Mock private ReplenishmentRepository replenishmentRepository;
    @Mock private TransportUnitRepository tuRepository;
    @Mock private StockRepository stockRepository;
    @Mock private InventoryService inventoryService;
    @Mock private WorkflowService workflowService;
    @Mock private ShortageResolver shortageResolver;
    @Mock private OperatorSummaryMapper summaryMapper;
    @Spy private PickingFlowService pickingFlowService;

    @InjectMocks
    private ReplenishmentOperatorStrategy strategy;

    private User operator;
    private Task task;
    private Allocation allocation;
    private Stock stock;
    private Replenishment replenishment;

    @BeforeEach
    void setUp() {
        operator = new User();
        ReflectionTestUtils.setField(operator, "id", 1L);

        Product product = new Product("Coca-Cola", "SKU-001", null, null);
        ReflectionTestUtils.setField(product, "id", 10L);

        Location sourceLocation = new Location("Pick A", "PICK-A", Zone.PICKING, null, true);
        ReflectionTestUtils.setField(sourceLocation, "id", 20L);

        stock = new Stock(product, sourceLocation, 60, 25, null, null);
        ReflectionTestUtils.setField(stock, "id", 30L);

        task = new Task(null, TaskType.REPLENISHMENT, 25);
        task.setOperator(operator);
        ReflectionTestUtils.setField(task, "id", 40L);

        allocation = new Allocation(task, stock, 25, Status.IN_PROGRESS);
        allocation.setPickedQuantity(20);
        ReflectionTestUtils.setField(allocation, "id", 50L);

        Location destinationLocation = new Location("Repl", "REPL-A", Zone.REPLENISHMENT, null, true);
        ReflectionTestUtils.setField(destinationLocation, "id", 60L);
        replenishment = new Replenishment(product, 25, destinationLocation);
        replenishment.setTask(task);
        ReflectionTestUtils.setField(replenishment, "id", 70L);
    }

    @Test
    void complete_withShortage_writesOffMissingSourceStock() {
        when(shortageResolver.resolveShortage(allocation, 5, "Replenishment")).thenReturn(List.of());
        when(workflowService.executeAllocationCompletion(allocation))
            .thenReturn(new AllocationCompletionResult(AllocationCompletionStatus.COMPLETED, TaskType.REPLENISHMENT, 70L));
        when(allocationRepository.findAllByTaskId(40L)).thenReturn(List.of(allocation));
        when(replenishmentRepository.findByTaskId(40L)).thenReturn(Optional.of(replenishment));
        when(tuRepository.existsByReplenishment(replenishment)).thenReturn(false);

        AllocationCompletionResponse response = strategy.complete(allocation, 20, operator);

        verify(inventoryService).recordShortageAdjustment(
            eq(stock),
            eq(40),
            eq(operator),
            eq(InventoryOperationType.REPLENISHMENT_SHORTAGE),
            eq("Replenishment shortage")
        );
        verify(shortageResolver).resolveShortage(allocation, 5, "Replenishment");
        assertThat(response.shortageQuantity()).isEqualTo(5);
    }
}
