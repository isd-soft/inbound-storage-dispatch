package com.isd.wms.service;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.ReplenishmentRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final AllocationRepository allocationRepository;
    private final StockRepository stockRepository;
    private final TaskRepository taskRepository;

    private final List<AllocationCompletionStrategy> allocationCompletionStrategies;
    private final List<StockAllocationStrategy> allocationStrategies;

    @Transactional
    public void generateAllocationsForTask(Task task, Long productId) {
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

        allocateStockToAllocations(task, availableStocks, productId);
    }

    private void allocateStockToAllocations(Task task,
                                           List<Stock> availableStocks,
                                           Long productId) {
        int quantityNeeded = task.getNotAllocatedQuantity();
        for (Stock stock : availableStocks) {
            if (quantityNeeded <= 0) break;

            int available = stock.getQuantity() - stock.getReservedQuantity();
            if (available <= 0) continue;

            int quantityToTake = Math.min(available, quantityNeeded);

            Allocation allocation = new Allocation(task, stock, quantityToTake, Status.CREATED);
            task.addAllocation(allocation);

            stock.setReservedQuantity(stock.getReservedQuantity() + quantityToTake);
            quantityNeeded -= quantityToTake;
        }

        if (quantityNeeded > 0) {
            throw new InvalidRequestException(
                    String.format("Insufficient stock for Product ID: %d. Missing %d pcs.",
                            productId, quantityNeeded)
            );
        }
    }

    @Transactional
    public void updateTask(Task task, Long productId) {
        allocationRepository.deleteByTaskId(task.getId());
        generateAllocationsForTask(task, productId);
    }

    @Transactional
    public AllocationCompletionResult executeAllocationCompletion(Allocation allocation) {
        Stock sourceStock = allocation.getStock();

        int quantityToMove = allocation.getPickedQuantity() != null ? allocation.getPickedQuantity() : allocation.getQuantity();

        sourceStock.removeQuantity(quantityToMove);
        stockRepository.save(sourceStock);

        Task task = allocation.getTask();

        AllocationCompletionStrategy strategy = allocationCompletionStrategies.stream()
                .filter(s -> s.support(task.getTaskType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No completion strategy found for task type: " + task.getTaskType()
                        ));

        strategy.handle(allocation);

        int taskFullyCompletedCount = taskRepository.markTaskAsCompleted(allocation.getTask().getId());

        if (taskFullyCompletedCount != 0) {
            strategy.updateStatus(task);
        }
        return strategy.result(task);
    }
}
