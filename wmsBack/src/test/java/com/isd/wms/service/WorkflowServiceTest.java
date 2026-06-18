package com.isd.wms.service;

import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.AllocationRepository  ;
import com.isd.wms.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    @Mock
    private AllocationRepository  allocationRepository ;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private WorkflowService workflowService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
    }

    @Test
    void generateAllocationsForTask_success_createsOneAllocation() {
        Stock stock = new Stock();
        stock.setId(10L);
        stock.setQuantity(100);
        stock.setReservedQuantity(0);

        when(stockRepository.findAvailableStocksByProductId(1L))
                .thenReturn(new ArrayList<>(List.of(stock)));

//        workflowService.generateAllocationsForTask(task, 1L, 50);

        verify(allocationRepository , times(1)).saveAll(anyList());
        assertThat(stock.getReservedQuantity()).isEqualTo(50);
    }

    @Test
    void generateAllocationsForTask_splitAllocations_createsTwoAllocations() {
        Stock stock1 = new Stock();
        stock1.setId(10L);
        stock1.setQuantity(50);
        stock1.setReservedQuantity(0);

        Stock stock2 = new Stock();
        stock2.setId(11L);
        stock2.setQuantity(50);
        stock2.setReservedQuantity(0);

        when(stockRepository.findAvailableStocksByProductId(1L))
                .thenReturn(new ArrayList<>(List.of(stock1, stock2)));

//        workflowService.generateAllocationsForTask(task, 1L, 70);

        ArgumentCaptor<List<Allocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(allocationRepository ).saveAll(captor.capture());

        List<Allocation> capturedAllocations = captor.getValue();
        assertThat(capturedAllocations).hasSize(2);
        assertThat(capturedAllocations.get(0).getQuantity()).isEqualTo(50);
        assertThat(capturedAllocations.get(1).getQuantity()).isEqualTo(20);

        assertThat(stock2.getReservedQuantity()).isEqualTo(50);
        assertThat(stock1.getReservedQuantity()).isEqualTo(20);
    }

    @Test
    void generateAllocationsForTask_insufficientStock_throwsException() {
        Stock stock = new Stock();
        stock.setId(10L);
        stock.setQuantity(10);
        stock.setReservedQuantity(0);

        when(stockRepository.findAvailableStocksByProductId(1L))
                .thenReturn(new ArrayList<>(List.of(stock)));

//        assertThatThrownBy(() -> workflowService.generateAllocationsForTask(task, 1L, 50))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("Insufficient stock");

        verify(allocationRepository , never()).saveAll(anyList());
    }
}
