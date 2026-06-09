package com.isd.wms.service;

import com.isd.wms.entity.Process;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.ProcessStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
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
    private final StockRepository stockRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public void generateProcessesForTask(Task task, Long productId, int requiredQuantity) {
        int remainingQuantity = requiredQuantity;
        List<Stock> availableStocks = stockRepository.findAvailableStocksByProductId(productId).reversed();
        List<Process> processesToSave = new ArrayList<>();

        availableStocks.sort((s1, s2) -> {
            int diff1 = s1.getQuantity() - s1.getReservedQuantity();
            int diff2 = s2.getQuantity() - s2.getReservedQuantity();
            return Integer.compare(diff1, diff2);
        });

        assignProcesses(task, productId, remainingQuantity, availableStocks, processesToSave);

        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
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
        processRepository.deleteByTaskId(task.getId());
        generateProcessesForTask(task, productId, requestedQuantity);
    }
}