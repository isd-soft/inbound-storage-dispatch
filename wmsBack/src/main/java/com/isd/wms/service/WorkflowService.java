package com.isd.wms.service;

import com.isd.wms.entity.*;
import com.isd.wms.entity.Process;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
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

    @Transactional
    public void generateProcessesForTask(Task task, Long productId, int remainingQuantity) {
        log.info("ALGO START: Generating execution processes for Task ID: {}. Target Product ID: {}, Required Qty: {}",
            task.getId(), productId, remainingQuantity);

        List<Stock> availableStocks = new ArrayList<>(stockRepository.findAvailableStocksByProductId(productId));
        List<Process> processesToSave = new ArrayList<>();
        log.debug("Found {} distinct stock lines available in database for Product ID: {}", availableStocks.size(), productId);

        availableStocks.sort((s1, s2) -> {
            int diff1 = s1.getQuantity() - s1.getReservedQuantity();
            int diff2 = s2.getQuantity() - s2.getReservedQuantity();
            return Integer.compare(diff1, diff2);
        });

        assignProcesses(task, productId, remainingQuantity, availableStocks, processesToSave);

        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
        log.info("ALGO SUCCESS: Successfully split Task ID {} into {} discrete workflow execution processes",
            task.getId(), processesToSave.size());
    }

    private static void assignProcesses(Task task, Long productId, int remainingQuantity, List<Stock> availableStocks, List<Process> processesToSave) {
        while (remainingQuantity > 0) {
            Stock bestStock = null;

            bestStock = searchForStock(availableStocks, remainingQuantity, bestStock);

            if (bestStock == null) {
                throw new InvalidRequestException("Insufficient stock for Product ID: " + productId);
            }

            int available = bestStock.getQuantity() - bestStock.getReservedQuantity();
            int quantityToTake = Math.min(available, remainingQuantity);

            Process process = new Process(task, bestStock, quantityToTake, Status.CREATED);
            processesToSave.add(process);

            log.debug("Task ID {}: Allocated {} pcs from Stock ID {} (Location: '{}')",
                task.getId(), quantityToTake, bestStock.getId(), bestStock.getLocation().getBarcode());

            bestStock.setReservedQuantity(bestStock.getReservedQuantity() + quantityToTake);
            remainingQuantity -= quantityToTake;
        }
    }

    private static Stock searchForStock(List<Stock> availableStocks, int remainingQuantity, Stock bestStock) {
        for (Stock stock : availableStocks) {
            int available = stock.getQuantity() - stock.getReservedQuantity();

            if (available <= 0) continue;

            if (available >= remainingQuantity) {
                bestStock = stock;
                break;
            }

            bestStock = stock;
        }
        return bestStock;
    }

    @Transactional
    public void updateTask(Task task, Long productId, Integer requestedQuantity) {
        log.warn("Task structure update triggered for Task ID: {}. Wiping out old processes and re-running allocation engine.", task.getId());
        processRepository.deleteByTaskId(task.getId());
        generateProcessesForTask(task, productId, requestedQuantity);
    }

    @Transactional
    public void executeProcessCompletion(Process process) {
        Stock sourceStock = process.getStock();
        int quantityToMove = process.getQuantity();

        log.info("Executing completion logic for Process ID: {} (Type: {}, Linked Task ID: {})",
            process.getId(), process.getTask().getTaskType(), process.getTask().getId());

        sourceStock.removeQuantity(quantityToMove);
        stockRepository.save(sourceStock);

        Task task = process.getTask();

        log.debug("Resolving functional process completion strategy for type: {}", task.getTaskType());
        processCompletionStrategies.stream()
                .filter(strategy -> strategy.support(task.getTaskType()))
                .findAny()
                .ifPresentOrElse(strategy -> strategy.handle(process),
                        () -> new RuntimeException("new exc"));

        List<Process> allProcesses = processRepository.findAllByTaskId(task.getId());
        boolean isTaskFullyCompleted = allProcesses.stream()
                .allMatch(p -> p.getStatus() == Status.COMPLETED || p.getId().equals(process.getId()));

        if (isTaskFullyCompleted) {
            log.info("All sub-processes for Task ID {} are complete. Elevating task status to COMPLETED.", task.getId());
            task.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(task);

            if (task.getTaskType() == TaskType.REPLENISHMENT) {
                Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId()).get();
                replenishment.setStatus(Status.COMPLETED);
                replenishmentRepository.save(replenishment);
            }
        }
    }
}
