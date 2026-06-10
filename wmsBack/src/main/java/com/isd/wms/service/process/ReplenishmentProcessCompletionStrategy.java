package com.isd.wms.service.process;

import com.isd.wms.entity.Location;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Stock;
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

        stockRepository.findByProductAndLocation(sourceStock.getProduct(), destinationLocation)
                .ifPresentOrElse(existingStock -> existingStock.addQuantity(quantityToMove), //todo: check if we need it
                        () ->  createStock(sourceStock, destinationLocation, quantityToMove));
    }

    private void createStock(Stock sourceStock, Location destinationLocation, int quantityToMove) {
        Stock newStock = Stock.builder()
                .product(sourceStock.getProduct())
                .location(destinationLocation)
                .quantity(quantityToMove)
                .reservedQuantity(0)
                .expirationDate(sourceStock.getExpirationDate())
                .manufactureDate(sourceStock.getManufactureDate())
                .build();
        stockRepository.save(newStock);
    }

    @Override
    public boolean support(TaskType taskType) {
        return TaskType.REPLENISHMENT == taskType;
    }
}
