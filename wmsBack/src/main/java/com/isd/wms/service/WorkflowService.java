package com.isd.wms.service;

import com.isd.wms.dto.process.ProcessCompletionResponse;
import com.isd.wms.dto.process.ProcessCompletionResult;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.service.allocation.StockAllocationStrategy;
import com.isd.wms.service.process.ProcessCompletionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final ProcessRepository processRepository;
    private final ReplenishmentRepository replenishmentRepository;
    private final StockRepository stockRepository;
    private final TaskRepository taskRepository;

    private final List<ProcessCompletionStrategy> processCompletionStrategies;
    private final List<StockAllocationStrategy> allocationStrategies;

    @Transactional
    public void generateProcessesForTask(Task task, Long productId, int remainingQuantity) {
        StockAllocationStrategy strategy = allocationStrategies.stream()
            .filter(s -> s.support(task.getTaskType()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No allocation strategy found for task type: " + task.getTaskType()));

        List<Stock> availableStocks = new ArrayList<>(
            stockRepository.findAvailableStocksByProductIdAndZone(productId, strategy.getSourceZone())
        );

        if (availableStocks.isEmpty()) {
            throw new InvalidRequestException(
                String.format("Insufficient stock for Product ID: %d in %s zone.", productId, strategy.getSourceZone().name())
            );
        }

        strategy.sortStocks(availableStocks);

        List<Process> processesToSave = allocateStockToProcesses(task, availableStocks, remainingQuantity, productId, strategy.getSourceZone().name());

        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
    }

    private List<Process> allocateStockToProcesses(Task task, List<Stock> availableStocks, int quantityNeeded, Long productId, String zoneName) {
        List<Process> processes = new ArrayList<>();
        int qtyNeeded = quantityNeeded;

        for (Stock stock : availableStocks) {
            if (qtyNeeded <= 0) break;

            int available = stock.getQuantity() - stock.getReservedQuantity();
            if (available <= 0) continue;

            int quantityToTake = Math.min(available, qtyNeeded);

            Process process = new Process(task, stock, quantityToTake, Status.CREATED);
            processes.add(process);

            stock.setReservedQuantity(stock.getReservedQuantity() + quantityToTake);
            qtyNeeded -= quantityToTake;
        }

        if (qtyNeeded > 0) {
            throw new InvalidRequestException(
                String.format("Insufficient stock for Product ID: %d in %s zone. Missing %d pcs.",
                    productId, zoneName, qtyNeeded)
            );
        }
        return processes;
    }

    @Transactional
    public void updateTask(Task task, Long productId, Integer requestedQuantity) {
        processRepository.deleteByTaskId(task.getId());
        generateProcessesForTask(task, productId, requestedQuantity);
    }

    @Transactional
    public ProcessCompletionResult executeProcessCompletion(Process process) {
        Stock sourceStock = process.getStock();

        int quantityToMove = process.getPickedQuantity() != null ? process.getPickedQuantity() : process.getQuantity();

        sourceStock.removeQuantity(quantityToMove);
        stockRepository.save(sourceStock);

        Task task = process.getTask();

        ProcessCompletionStrategy strategy = processCompletionStrategies.stream()
            .filter(s -> s.support(task.getTaskType()))
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException(
                    "No completion strategy found for task type: " + task.getTaskType()
                ));

        strategy.handle(process);

        int taskFullyCompleted = taskRepository.markTaskAsCompleted(process.getTask().getId());

        if (taskFullyCompleted != 0) {
            strategy.updateStatus(task);
        }
        return strategy.result(task);
    }
}
