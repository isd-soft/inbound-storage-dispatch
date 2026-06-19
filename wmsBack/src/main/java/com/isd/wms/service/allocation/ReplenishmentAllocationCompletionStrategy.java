package com.isd.wms.service.allocation;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.*;
import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplenishmentAllocationCompletionStrategy implements AllocationCompletionStrategy {

    private final ReplenishmentRepository replenishmentRepository;
    private final StockRepository stockRepository;

    @Override
    public void handle(Allocation allocation) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(allocation.getTask().getId())
            .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));

        Location destinationLocation = replenishment.getDestinationLocation();
        Stock sourceStock = allocation.getStock();

        Integer quantityToMove = allocation.getQuantity();
        Product product = sourceStock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Source stock product is required"));

        stockRepository.findByLocationId(destinationLocation.getId())
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
        return replenishmentRepository.updateReplenishmentStatusByTask(task) > 0;
    }

    @Override
    public AllocationCompletionResult result(Task task) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));
        return new AllocationCompletionResult(
            replenishment.getStatus() == Status.COMPLETED ? AllocationCompletionStatus.COMPLETED : AllocationCompletionStatus.IN_PROGRESS,
            TaskType.REPLENISHMENT,
            replenishment.getId()
        );
    }

    @Override
    public boolean support(TaskType taskType) {
        return TaskType.REPLENISHMENT == taskType;
    }
}
