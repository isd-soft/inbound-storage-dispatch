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
import java.util.Comparator;
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

        availableStocks.sort(Comparator.comparing(Stock::getQuantity));

        int lastIndex = availableStocks.size() - 1;

        while (remainingQuantity > 0) {
            for (int i = 0; i <= lastIndex; i++) {
                Stock stock = availableStocks.get(i);

                if (i < lastIndex && availableStocks.get(i + 1).getQuantity() > remainingQuantity) {
                    continue;
                }

                int quantityToTake = Math.min(stock.getQuantity() - stock.getReservedQuantity(), remainingQuantity);

                processesToSave.add(
                        Process.builder()
                                .task(task)
                                .stock(stock)
                                .quantity(quantityToTake)
                                .status(ProcessStatus.CREATED)
                                .build()
                );

                stock.setReservedQuantity(stock.getReservedQuantity() + quantityToTake);

                remainingQuantity -= quantityToTake;
                break;
            }
        }

        if (remainingQuantity > 0) {
            throw new InvalidRequestException("Insufficient unreserved stock for Product ID: " + productId);
        }
        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
    }

    @Transactional
    public void updateTask(Task task, Long productId, Integer requestedQuantity) {
        processRepository.deleteByTaskId(task.getId());
        generateProcessesForTask(task, productId, requestedQuantity);
    }
}