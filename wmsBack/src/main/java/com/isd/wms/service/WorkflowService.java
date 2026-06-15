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
        log.info("ALGO START: Generating execution processes for Task ID: {}. Target Product ID: {}, Required Qty: {}",
            task.getId(), productId, remainingQuantity);

        StockAllocationStrategy strategy = allocationStrategies.stream()
            .filter(s -> s.support(task.getTaskType()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No allocation strategy found for task type: " + task.getTaskType()));

        List<Stock> availableStocks = new ArrayList<>(
            stockRepository.findAvailableStocksByProductIdAndZone(productId, strategy.getSourceZone())
        );

        log.debug("Found {} distinct stock lines available in database for Product ID: {} within zone: {}", 
            availableStocks.size(), productId, strategy.getSourceZone());

        if (availableStocks.isEmpty()) {
            throw new InvalidRequestException(
                String.format("Insufficient stock for Product ID: %d in %s zone.", productId, strategy.getSourceZone().name())
            );
        }

        strategy.sortStocks(availableStocks);

        List<Process> processesToSave = allocateStockToProcesses(task, availableStocks, remainingQuantity, productId, strategy.getSourceZone().name());

        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
        
        log.info("ALGO SUCCESS: Successfully split Task ID {} into {} discrete workflow execution processes",
            task.getId(), processesToSave.size());
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

            log.debug("Task ID {}: Allocated {} pcs from Stock ID {} (Location: '{}')",
                task.getId(), quantityToTake, stock.getId(), stock.getLocation().getBarcode());

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
        log.warn("Task structure update triggered for Task ID: {}. Wiping out old processes and re-running allocation engine.", task.getId());
        processRepository.deleteByTaskId(task.getId());
        generateProcessesForTask(task, productId, requestedQuantity);
    }

    @Transactional
    public ProcessCompletionResult executeProcessCompletion(Process process) {
        Stock sourceStock = process.getStock();
        int quantityToMove = process.getPickedQuantity() != null ? process.getPickedQuantity() : process.getQuantity();

        log.info("Executing completion logic for Process ID: {} (Type: {}, Linked Task ID: {})",
            process.getId(), process.getTask().getTaskType(), process.getTask().getId());

        sourceStock.removeQuantity(quantityToMove);
        stockRepository.save(sourceStock);

        Task task = process.getTask();

        log.debug("Resolving functional process completion strategy for type: {}", task.getTaskType());
        ProcessCompletionStrategy strategy = processCompletionStrategies.stream()
            .filter(s -> s.support(task.getTaskType()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No completion strategy found for task type: " + task.getTaskType()
            ));

        strategy.handle(process);

        int taskFullyCompleted = taskRepository.markTaskAsCompleted(process.getTask().getId());

        if (taskFullyCompleted != 0) {
            log.info("All sub-processes for Task ID {} are complete. Elevating task status and components.", task.getId());
            strategy.updateStatus(task);
        }
        
        return strategy.result(task);
    }
}