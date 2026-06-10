package com.isd.wms.service;

import com.isd.wms.entity.*;
import com.isd.wms.entity.Process;
import com.isd.wms.enums.ProcessStatus;
import com.isd.wms.enums.ReplenishmentStatus;
import com.isd.wms.enums.TaskStatus;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final ProcessRepository processRepository;
    private final ReplenishmentRepository replenishmentRepository;
    private final StockRepository stockRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void generateProcessesForTask(Task task, Long productId, int requiredQuantity) {
        int remainingQuantity = requiredQuantity;

        List<Stock> availableStocks = new ArrayList<>(stockRepository.findAvailableStocksByProductId(productId));
        List<Process> processesToSave = new ArrayList<>();

        availableStocks.sort((s1, s2) -> {
            int diff1 = s1.getQuantity() - s1.getReservedQuantity();
            int diff2 = s2.getQuantity() - s2.getReservedQuantity();
            return Integer.compare(diff1, diff2);
        });

        while (remainingQuantity > 0) {
            Stock bestStock = null;

            for (Stock stock : availableStocks) {
                int available = stock.getQuantity() - stock.getReservedQuantity();

                if (available <= 0) continue;

                if (available >= remainingQuantity) {
                    bestStock = stock;
                    break;
                }

                bestStock = stock;
            }

            if (bestStock == null) {
                throw new InvalidRequestException("Insufficient stock for Product ID: " + productId);
            }

            int available = bestStock.getQuantity() - bestStock.getReservedQuantity();
            int quantityToTake = Math.min(available, remainingQuantity);

            processesToSave.add(
                    Process.builder()
                            .task(task)
                            .stock(bestStock)
                            .quantity(quantityToTake)
                            .status(ProcessStatus.CREATED)
                            .build()
            );

            bestStock.setReservedQuantity(bestStock.getReservedQuantity() + quantityToTake);
            remainingQuantity -= quantityToTake;
        }

        if (remainingQuantity > 0) {
            throw new InvalidRequestException(
                    "Insufficient unreserved stock for Product ID: " + productId
            );
        }

        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
    }

    @Transactional
    public void updateTask(Task task, Long productId, Integer requestedQuantity) {
        processRepository.deleteByTaskId(task.getId());
        generateProcessesForTask(task, productId, requestedQuantity);
    }

    @Transactional
    public void executeProcessCompletion(Process process) {
        Stock sourceStock = process.getStock();
        int quantityToMove = process.getQuantity();

        sourceStock.setQuantity(sourceStock.getQuantity() - quantityToMove);
        sourceStock.setReservedQuantity(sourceStock.getReservedQuantity() - quantityToMove);
        stockRepository.save(sourceStock);

        Task task = process.getTask();

        if (task.getTaskType() == TaskType.REPLENISHMENT) {
            Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
                    .orElseThrow(() -> new RuntimeException("Replenishment not found for task"));

            Location destinationLocation = replenishment.getDestinationLocation();
            Product product = sourceStock.getProduct();

            Optional<Stock> destStockOpt = stockRepository.findByProductIdAndLocationId(product.getId(), destinationLocation.getId());

            if (destStockOpt.isPresent()) {
                Stock destStock = destStockOpt.get();
                destStock.setQuantity(destStock.getQuantity() + quantityToMove);
                stockRepository.save(destStock);
            } else {
                Stock newStock = Stock.builder()
                        .product(product)
                        .location(destinationLocation)
                        .quantity(quantityToMove)
                        .reservedQuantity(0)
                        .manufactureDate(sourceStock.getManufactureDate())
                        .build();
                stockRepository.save(newStock);
            }
        }

        List<Process> allProcesses = processRepository.findAllByTaskId(task.getId());
        boolean isTaskFullyCompleted = allProcesses.stream()
                .allMatch(p -> p.getStatus() == ProcessStatus.COMPLETED || p.getId().equals(process.getId()));

        if (isTaskFullyCompleted) {
            task.setStatus(TaskStatus.COMPLETED);
            taskRepository.save(task);

            if (task.getTaskType() == TaskType.REPLENISHMENT) {
                Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId()).get();
                replenishment.setStatus(ReplenishmentStatus.COMPLETED);
                replenishmentRepository.save(replenishment);
            }
        }
    }
}