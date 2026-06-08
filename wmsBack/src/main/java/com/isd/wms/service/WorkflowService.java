package com.isd.wms.service;

import com.isd.wms.entity.Process;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.ProcessStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.StockRepository;
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

    @Transactional
    public void generateProcessesForTask(Task task, Long productId, int requiredQuantity) {
        int remainingQuantity = requiredQuantity;
        List<Stock> availableStocks = stockRepository.findAvailableStocksByProductId(productId);
        List<Process> processesToSave = new ArrayList<>();

        for (Stock stock : availableStocks) {
            if (remainingQuantity <= 0) break;

            int availableInThisStock = stock.getQuantity() - stock.getReservedQuantity();
            int quantityToTake = Math.min(availableInThisStock, remainingQuantity);

            Process process = Process.builder()
                    .task(task)
                    .stock(stock)
                    .quantity(quantityToTake)
                    .status(ProcessStatus.CREATED)
                    .build();

            processesToSave.add(process);
            stock.setReservedQuantity(stock.getReservedQuantity() + quantityToTake);
            remainingQuantity -= quantityToTake;
        }

        if (remainingQuantity > 0) {
            throw new InvalidRequestException("Insufficient unreserved stock for Product ID: " + productId);
        }
        processRepository.saveAll(processesToSave);
        stockRepository.saveAll(availableStocks);
    }

}