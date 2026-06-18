package com.isd.wms.service;

import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Stock;
import com.isd.wms.enums.Status;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQuantityService {

    private final StockRepository stockRepository;
    private final AllocationRepository allocationRepository;
    private final WorkflowService workflowService;

    @Transactional
    public void edit(Long stockId, int newQuantity) {
        Stock stock = stockRepository.findById(stockId).orElseThrow();
        List<Allocation> allocations = allocationRepository.findAllByStockAndStatusIn(stock, List.of(Status.ASSIGNED));
        stock.setReservedQuantity(0);
        stock.setQuantity(newQuantity);
        allocations.forEach(this::reallocate);
    }

    private void reallocate(Allocation allocation) {
        allocationRepository.delete(allocation);
        workflowService.generateAllocationsForTask(allocation.getTask(), allocation.getProduct().getId());
    }


}
