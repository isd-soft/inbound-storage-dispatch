package com.isd.wms.service.allocation;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.*;
import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplenishmentAllocationCompletionStrategyTest {

    @Mock private ReplenishmentRepository replenishmentRepository;
    @Mock private AllocationRepository allocationRepository;
    @Mock private StockRepository stockRepository;

    @InjectMocks
    private ReplenishmentAllocationCompletionStrategy strategy;

    private Task task;
    private Replenishment replenishment;
    private Allocation allocation;
    private Stock sourceStock;

    @BeforeEach
    void setUp() {
        task = new Task();
        ReflectionTestUtils.setField(task, "id", 1L);

        Product product = new Product("Test Product", "BARCODE", null, null);
        ReflectionTestUtils.setField(product, "id", 10L);

        Location destinationLocation = new Location();
        ReflectionTestUtils.setField(destinationLocation, "id", 2L);

        replenishment = new Replenishment(product, 10, destinationLocation);
        ReflectionTestUtils.setField(replenishment, "id", 10L);
        replenishment.setTask(task);

        sourceStock = new Stock();
        sourceStock.setProduct(product);
        sourceStock.setQuantity(20);
        sourceStock.setReservedQuantity(10);

        allocation = new Allocation();
        allocation.setQuantity(10);
        allocation.setStock(sourceStock);
        allocation.setTask(task);
        ReflectionTestUtils.setField(allocation, "id", 100L);
    }

    @Test
    void handle_withShortage_manuallyDecrementsStockAndSaves() {
        allocation.setPickedQuantity(7);

        when(replenishmentRepository.findByTaskId(1L)).thenReturn(Optional.of(replenishment));

        when(stockRepository.findByLocationId(2L)).thenReturn(Optional.empty());

        strategy.handle(allocation);

        verify(stockRepository).save(sourceStock);
        assertThat(sourceStock.getQuantity()).isEqualTo(10);
        assertThat(sourceStock.getReservedQuantity()).isZero();
    }

    @Test
    void updateStatus_withPartialHistory_setsPartiallyCompleted() {
        allocation.setStatus(Status.SHORTAGE);

        when(replenishmentRepository.findByTaskId(1L)).thenReturn(Optional.of(replenishment));
        when(allocationRepository.findAllByTaskId(1L)).thenReturn(List.of(allocation));

        boolean result = strategy.updateStatus(task);

        assertThat(result).isTrue();
        assertThat(replenishment.getStatus()).isEqualTo(Status.PARTIALLY_COMPLETED);
        verify(replenishmentRepository).save(replenishment);
    }

    @Test
    void result_returnsCorrectStatusBasedOnReplenishment() {
        replenishment.setStatus(Status.COMPLETED);
        when(replenishmentRepository.findByTaskId(1L)).thenReturn(Optional.of(replenishment));

        AllocationCompletionResult result = strategy.result(task);

        assertThat(result.status()).isEqualTo(AllocationCompletionStatus.COMPLETED);
        assertThat(result.taskType()).isEqualTo(TaskType.REPLENISHMENT);
    }
}
