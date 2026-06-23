package com.isd.wms.service.allocation;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.*;
import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Completion strategy for replenishment allocations.
 * <p>
 * When a replenishment allocation is completed, this strategy handles shortage
 * adjustments if the picked quantity is less than requested.
 * The physical movement of stock to the destination location is intentionally
 * NOT handled here; it occurs during the dispatch (drop-off) phase.
 * </p>
 * <p>
 * After processing, the replenishment status is updated to COMPLETED,
 * PARTIALLY_COMPLETED, or CANCELED based on the state of its allocations.
 * </p>
 *
 * @see Replenishment
 * @see Stock
 * @see Location
 */
@Component
@RequiredArgsConstructor
public class ReplenishmentAllocationCompletionStrategy implements AllocationCompletionStrategy {

    private final ReplenishmentRepository replenishmentRepository;
    private final AllocationRepository allocationRepository;
    private final StockRepository stockRepository;
    private final InventoryService inventoryService;

    @Override
    public void handle(Allocation allocation) {
        Stock sourceStock = allocation.getStock();

        if (allocation.getPickedQuantity().isPresent() && allocation.getPickedQuantity().get() < allocation.getQuantity()) {
            int missingQuantity = allocation.getQuantity() - allocation.getPickedQuantity().get();
            User operator = allocation.getTask().getOperator()
                .orElseThrow(() -> new IllegalStateException("Task must have an assigned operator"));

            inventoryService.recordShortageAdjustment(
                sourceStock,
                missingQuantity,
                operator,
                InventoryOperationType.REPLENISHMENT_SHORTAGE,
                "Replenishment shortage"
            );
        }
    }

    @Override
    public boolean updateStatus(Task task) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));
        List<Allocation> allocations = allocationRepository.findAllByTaskId(task.getId());

        boolean hasPending = allocations.stream().anyMatch(allocation ->
            allocation.getStatus() == Status.CREATED
                || allocation.getStatus() == Status.ASSIGNED
                || allocation.getStatus() == Status.IN_PROGRESS
        );

        if (hasPending) {
            replenishment.setStatus(Status.IN_PROGRESS);
        } else {
            boolean allCanceled = !allocations.isEmpty() && allocations.stream().allMatch(allocation -> allocation.getStatus() == Status.CANCELED);
            boolean hasPartialHistory = allocations.stream().anyMatch(allocation ->
                allocation.getStatus() == Status.CANCELED
                    || allocation.getStatus() == Status.SHORTAGE
                    || allocation.getStatus() == Status.PARTIALLY_COMPLETED
                    || resolvedDeliveredQuantity(allocation) < Optional.ofNullable(allocation.getQuantity()).orElse(0)
            );

            replenishment.setStatus(allCanceled ? Status.CANCELED : hasPartialHistory ? Status.PARTIALLY_COMPLETED : Status.COMPLETED);
        }

        replenishmentRepository.save(replenishment);
        return true;
    }

    @Override
    public AllocationCompletionResult result(Task task) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));
        return new AllocationCompletionResult(
            replenishment.getStatus() == Status.COMPLETED
                || replenishment.getStatus() == Status.PARTIALLY_COMPLETED
                || replenishment.getStatus() == Status.CANCELED
                ? AllocationCompletionStatus.COMPLETED
                : AllocationCompletionStatus.IN_PROGRESS,
            TaskType.REPLENISHMENT,
            replenishment.getId()
        );
    }

    @Override
    public boolean support(TaskType taskType) {
        return TaskType.REPLENISHMENT == taskType;
    }

    private int resolvedDeliveredQuantity(Allocation allocation) {
        if (allocation.getPickedQuantity().isPresent()) {
            return allocation.getPickedQuantity().orElse(0);
        }
        return allocation.getStatus() == Status.CANCELED ? 0 : Optional.ofNullable(allocation.getQuantity()).orElse(0);
    }
}
