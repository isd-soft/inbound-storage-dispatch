package com.isd.wms.service.process;

import com.isd.wms.entity.*;
import com.isd.wms.entity.Process;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplenishmentProcessCompletionStrategy implements ProcessCompletionStrategy {

    private final ReplenishmentRepository replenishmentRepository;
    private final StockRepository stockRepository;

    @Override
    public void handle(Process process) {
        Replenishment replenishment = replenishmentRepository.findByTaskId(process.getTask().getId())
                .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));

        Location destinationLocation = replenishment.getDestinationLocation();
        Stock sourceStock = process.getStock();

        Integer quantityToMove = process.getQuantity();
        Product product = sourceStock.getProduct()
                .orElseThrow(() -> new IllegalStateException("Source stock product is required"));

        stockRepository.findByProductAndLocation(product, destinationLocation)
                .ifPresentOrElse(existingStock -> {
                        existingStock.addQuantity(quantityToMove);
                        existingStock.updateDate(sourceStock.getManufactureDate(), sourceStock.getExpirationDate());
                    }, //todo: check if we need it
                        () ->  createStock(sourceStock, product, destinationLocation, quantityToMove));
    }

    private void createStock(Stock sourceStock, Product product, Location destinationLocation, int quantityToMove) {
        Stock newStock = new Stock(product, destinationLocation, quantityToMove, sourceStock.getManufactureDate(), sourceStock.getExpirationDate());
        stockRepository.save(newStock);
    }

    @Override
    public void updateStatus(Task task) {
        if (task.getStatus() != TaskStatus.COMPLETED) {
            return;
        }

        if (replenishmentRepository.updateReplenishmentStatusByTask(task) == 0) {
            throw new RuntimeException("Replenishment not found");
        }
    }

    @Override
    public boolean support(TaskType taskType) {
        return TaskType.REPLENISHMENT == taskType;
    }
}
