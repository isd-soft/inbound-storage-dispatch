package com.isd.wms.service.process;

import com.isd.wms.entity.Location;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Stock;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReplenishmentProcessCompletionStrategy implements ProcessCompletionStrategy {

    private final ReplenishmentRepository replenishmentRepository;
    private final StockRepository stockRepository;

    @Override
    public void handle(Process process) {
        log.info("Executing completion strategy for Replenishment Process ID: {}", process.getId());

        Replenishment replenishment = replenishmentRepository.findByTaskId(process.getTask().getId())
            .orElseThrow(() -> {
                log.error("CRITICAL: Replenishment records not found for Task ID: {}", process.getTask().getId());
                return new RuntimeException("Replenishment not found for task");
            });

        Location destinationLocation = replenishment.getDestinationLocation();
        Stock sourceStock = process.getStock();
        Location sourceLocation = sourceStock.getLocation();

        Integer quantityToMove = process.getQuantity();
        Product product = sourceStock.getProduct()
            .orElseThrow(() -> {
                log.error("CRITICAL: Source stock ID: {} does not have a product assigned", sourceStock.getId());
                return new IllegalStateException("Source stock product is required");
            });

        log.info("Moving quantity: {} of Product: '{}' from Location: '{}' to Destination Location: '{}'",
            quantityToMove, product.getName(), sourceLocation.getName(), destinationLocation.getName());

        stockRepository.findByProductAndLocation(product, destinationLocation)
            .ifPresentOrElse(
                existingStock -> {
                    log.info("Target stock found at destination. Increasing quantity by {} (Old quantity: {}, New quantity: {})",
                        quantityToMove, existingStock.getQuantity(), existingStock.getQuantity() + quantityToMove);
                    existingStock.addQuantity(quantityToMove); //todo: check if we need it
                },
                () -> {
                    log.info("No existing stock found at destination. Creating a new stock record.");
                    createStock(sourceStock, product, destinationLocation, quantityToMove);
                }
            );
    }

    private void createStock(Stock sourceStock, Product product, Location destinationLocation, int quantityToMove) {
        Stock newStock = new Stock(product, destinationLocation);
        newStock.setQuantity(quantityToMove);
        newStock.setExpirationDate(sourceStock.getExpirationDate());
        newStock.setManufactureDate(sourceStock.getManufactureDate());

        Stock savedStock = stockRepository.save(newStock);
        log.info("Successfully created new Stock ID: {} at Location: '{}'", savedStock.getId(), destinationLocation.getName());
    }

    @Override
    public boolean support(TaskType taskType) {
        return TaskType.REPLENISHMENT == taskType;
    }
}
