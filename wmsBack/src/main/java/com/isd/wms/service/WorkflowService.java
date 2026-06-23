package com.isd.wms.service;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.service.allocation.AllocationCompletionStrategy;
import com.isd.wms.service.allocation.StockAllocationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Core service that orchestrates the workflow for task execution, including
 * allocation generation and completion handling.
 * <p>
 * The service uses pluggable strategies for allocation generation
 * ({@link StockAllocationStrategy}) and completion handling
 * ({@link AllocationCompletionStrategy}) based on the task type.
 * </p>
 * <p>
 * When a task is created or updated, allocations are generated from available
 * stock in the appropriate zone. When an allocation is completed, the service
 * determines the final outcome (full completion, partial shortage, etc.) and
 * updates the task status accordingly.
 * </p>
 *
 * @see Task
 * @see Allocation
 * @see Stock
 * @see StockAllocationStrategy
 * @see AllocationCompletionStrategy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final AllocationRepository allocationRepository;
    private final StockRepository stockRepository;
    private final TaskRepository taskRepository;

    private final List<AllocationCompletionStrategy> allocationCompletionStrategies;
    private final List<StockAllocationStrategy> allocationStrategies;

    /**
     * Generates allocations for a task by allocating available stock from the
     * appropriate zone, using the matching strategy.
     *
     * @param task the task for which to generate allocations
     * @param productId the product ID
     * @param remainingQuantity the quantity to allocate
     * @throws InvalidRequestException if insufficient stock is getAvailableQuantity
     */
    @Transactional
    public void generateAllocationsForTask(Task task, Long productId, int remainingQuantity) {
        StockAllocationStrategy strategy = allocationStrategies.stream()
            .filter(s -> s.support(task.getTaskType()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No allocation strategy found for task type: "
                + task.getTaskType()));

        List<Stock> availableStocks = new ArrayList<>(
            stockRepository.findAvailableStocksByProductIdAndZone(productId, strategy.getSourceZone())
        );

        if (availableStocks.isEmpty()) {
            throw new InvalidRequestException(
                String.format("Insufficient stock for Product ID: %d in %s zone.", productId,
                    strategy.getSourceZone().name())
            );
        }

        strategy.sortStocks(availableStocks);

        List<Allocation> allocationsToSave = allocateStockToAllocations(
            task, availableStocks, remainingQuantity, productId, strategy.getSourceZone().name());

        allocationRepository.saveAll(allocationsToSave);
        stockRepository.saveAll(availableStocks);
    }

    private List<Allocation> allocateStockToAllocations(
        Task task,
        List<Stock> availableStocks,
        int quantityNeeded,
        Long productId,
        String zoneName
    ) {
        List<Allocation> allocations = new ArrayList<>();
        int qtyNeeded = quantityNeeded;

        for (Stock stock : availableStocks) {
            if (qtyNeeded <= 0) break;

            if (stock.getAvailableQuantity() <= 0) continue;

            int quantityToTake = Math.min(stock.getAvailableQuantity(), qtyNeeded);

            Allocation allocation = new Allocation(task, stock, quantityToTake, Status.CREATED);
            allocations.add(allocation);

            stock.setReservedQuantity(stock.getReservedQuantity() + quantityToTake);
            qtyNeeded -= quantityToTake;
        }

        if (qtyNeeded > 0) {
            throw new InvalidRequestException(
                String.format("Insufficient stock for Product ID: %d in %s zone. Missing %d pcs.",
                    productId, zoneName, qtyNeeded)
            );
        }
        return allocations;
    }

    /**
     * Updates an existing task by regenerating allocations for a new product/quantity.
     * Old allocations are removed and reserved stock is released before new ones are created.
     *
     * @param task the task to update
     * @param productId the new product ID
     * @param requestedQuantity the new requested quantity
     */
    @Transactional
    public void updateTask(Task task, Long productId, Integer requestedQuantity) {
        List<Allocation> oldAllocations = allocationRepository.findAllByTaskId(task.getId());
        for (Allocation allocation : oldAllocations) {
            Stock stock = allocation.getStock();
            int newReserved = Math.max(0, stock.getReservedQuantity() - allocation.getQuantity());
            stock.setReservedQuantity(newReserved);
            stockRepository.save(stock);
        }

        allocationRepository.deleteAllInBatch(oldAllocations);
        allocationRepository.flush();

        generateAllocationsForTask(task, productId, requestedQuantity);
    }

    /**
     * Completes an allocation, reducing stock and invoking the appropriate completion
     * strategy based on the task type.
     *
     * @param allocation the allocation to complete
     * @return a result object containing the outcome (completed, shortage, etc.)
     */
    @Transactional
    public AllocationCompletionResult executeAllocationCompletion(Allocation allocation) {
        Stock sourceStock = allocation.getStock();

        int quantityToMove = allocation.getPickedQuantity().orElseGet(allocation::getQuantity);

        // НАШ ФИКС: Списываем физический остаток всегда, если мы хоть что-то собрали!
        // Никаких проверок partialPick перед списанием
        if (quantityToMove > 0) {
            sourceStock.removeQuantity(quantityToMove);
            if (sourceStock.getQuantity() == 0 && sourceStock.getReservedQuantity() == 0) {
                sourceStock.setAvailable(Boolean.FALSE);
            } else {
                stockRepository.save(sourceStock);
            }
        }

        Task task = allocation.getTask();

        AllocationCompletionStrategy strategy = allocationCompletionStrategies.stream()
            .filter(s -> s.support(task.getTaskType()))
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException(
                    "No completion strategy found for task type: " + task.getTaskType()
                ));

        strategy.handle(allocation);

        taskRepository.markTaskAsCompleted(allocation.getTask().getId());
        strategy.updateStatus(task);
        return strategy.result(task);
    }
}
