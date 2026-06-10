package com.isd.wms.service;

import com.isd.wms.entity.Process;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.ProcessRepository;
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
    private ProcessRepository processRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private WorkflowService workflowService;

    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
    }

    @Test
    void generateProcessesForTask_success_createsOneProcess() {
        Stock stock = Stock.builder().id(10L).quantity(100).reservedQuantity(0).build();
        when(stockRepository.findAvailableStocksByProductId(1L))
                .thenReturn(new ArrayList<>(List.of(stock)));

        workflowService.generateProcessesForTask(task, 1L, 50);

        verify(processRepository, times(1)).saveAll(anyList());
        assertThat(stock.getReservedQuantity()).isEqualTo(50);
    }

    @Test
    void generateProcessesForTask_splitProcesses_createsTwoProcesses() {
        Stock stock1 = Stock.builder().id(10L).quantity(50).reservedQuantity(0).build();
        Stock stock2 = Stock.builder().id(11L).quantity(50).reservedQuantity(0).build();
        when(stockRepository.findAvailableStocksByProductId(1L))
                .thenReturn(new ArrayList<>(List.of(stock1, stock2)));

        workflowService.generateProcessesForTask(task, 1L, 70);

        ArgumentCaptor<List<Process>> captor = ArgumentCaptor.forClass(List.class);
        verify(processRepository).saveAll(captor.capture());

        List<Process> capturedProcesses = captor.getValue();
        assertThat(capturedProcesses).hasSize(2);
        assertThat(capturedProcesses.get(0).getQuantity()).isEqualTo(50);
        assertThat(capturedProcesses.get(1).getQuantity()).isEqualTo(20);

        assertThat(stock2.getReservedQuantity()).isEqualTo(50);
        assertThat(stock1.getReservedQuantity()).isEqualTo(20);
    }

    @Test
    void generateProcessesForTask_insufficientStock_throwsException() {
        Stock stock = Stock.builder().id(10L).quantity(10).reservedQuantity(0).build();
        when(stockRepository.findAvailableStocksByProductId(1L))
                .thenReturn(new ArrayList<>(List.of(stock)));

        assertThatThrownBy(() -> workflowService.generateProcessesForTask(task, 1L, 50))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Insufficient stock");

        verify(processRepository, never()).saveAll(anyList());
    }
}