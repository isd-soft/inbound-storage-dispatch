package com.isd.wms.service.allocation;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.*;
import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

/**
 * Completion strategy for replenishment allocations.
 * <p>
 * When a replenishment allocation is completed, this strategy moves the picked
 * quantity from the source stock (REPL zone) to the destination location
 * (picking zone). If the destination location already contains stock of the same
 * product, the quantity is added; if it contains a different product, an error
 * is thrown (unless the location is empty).
 * </p>
 * <p>
 * After the move, the replenishment status is updated to COMPLETED if all
 * allocations are done.
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

    @Override
    public void handle(Allocation allocation) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(allocation.getTask().getId())
            .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));

        Location destinationLocation = replenishment.getDestinationLocation();
        Stock sourceStock = allocation.getStock();

        Integer quantityToMove = allocation.getPickedQuantity().orElse(allocation.getQuantity());
        Product product = sourceStock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Source stock product is required"));

        if (allocation.getPickedQuantity().isPresent()) {
            allocation.getPickedQuantity();
        }

        if (quantityToMove <= 0) {
            return;
        }

        stockRepository.findByLocationIdAndProductId(destinationLocation.getId(), product.getId())
            .ifPresentOrElse(existingStock -> {
                    Product existingProduct = existingStock.getProduct().orElse(null);

                    if (existingProduct != null && !existingProduct.getId().equals(product.getId())) {
                        if (existingStock.getQuantity() == 0 && existingStock.getReservedQuantity() == 0) {
                            existingStock.setProduct(product);
                            existingStock.setQuantity(quantityToMove);
                            existingStock.updateDate(sourceStock.getManufactureDate(), sourceStock.getExpirationDate());
                        } else {
                            throw new IllegalStateException("Location " + destinationLocation.getBarcode() + " is already occupied by a different product!");
                        }
                    } else {
                        if (existingProduct == null) existingStock.setProduct(product);
                        existingStock.addQuantity(quantityToMove);
                        existingStock.updateDate(sourceStock.getManufactureDate(), sourceStock.getExpirationDate());
                    }
                },
                () -> createStock(sourceStock, product, destinationLocation, quantityToMove));
    }

    private void createStock(Stock sourceStock, Product product, Location destinationLocation, int quantityToMove) {
        Stock newStock = new Stock(product, destinationLocation, quantityToMove, sourceStock.getManufactureDate(), sourceStock.getExpirationDate());
        stockRepository.save(newStock);
    }

    @Override
    public boolean updateStatus(Task task) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));
        List<Allocation> allocations = allocationRepository.findAllByTaskId(task.getId());

        boolean allCanceled = !allocations.isEmpty() && allocations.stream().allMatch(allocation -> allocation.getStatus() == Status.CANCELED);
        boolean hasPartialHistory = allocations.stream().anyMatch(allocation ->
            allocation.getStatus() == Status.CANCELED
                || allocation.getStatus() == Status.SHORTAGE
                || allocation.getStatus() == Status.PARTIALLY_COMPLETED
                || resolvedDeliveredQuantity(allocation) < Optional.ofNullable(allocation.getQuantity()).orElse(0)
        );

        replenishment.setStatus(allCanceled ? Status.CANCELED : hasPartialHistory ? Status.PARTIALLY_COMPLETED : Status.COMPLETED);
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
