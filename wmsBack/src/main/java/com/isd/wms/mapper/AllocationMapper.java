package com.isd.wms.mapper;

import com.isd.wms.dto.allocation.AllocationOperatorResponse;
import com.isd.wms.dto.allocation.AllocationResponse;
import com.isd.wms.dto.allocation.ShortAllocationResponse;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import org.springframework.stereotype.Component;

@Component
public class AllocationMapper {

    public AllocationOperatorResponse toOperatorResponse(Allocation allocation, Integer total, Integer current, String order) {
        return new AllocationOperatorResponse(
            total,
            current,
            order,
            allocation.getTask().getTaskType().name(),
            null,
            toShortResponse(allocation)
        );
    }

    public ShortAllocationResponse toShortResponse(Allocation allocation) {
        Stock stock = allocation.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new ShortAllocationResponse(
            allocation.getId(),
            product.getName(),
            product.getBarcode(),
            stock.getLocation().getBarcode(),
            stock.getLocation().getBarcode(),
            allocation.getQuantity()
        );
    }

    public AllocationResponse toResponse(Allocation allocation) {
        Stock stock = allocation.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new AllocationResponse(
            allocation.getId(),
            product.getName(),
            product.getBarcode(),
            stock.getLocation().getBarcode(),
            allocation.getQuantity(),
            allocation.getStatus()
        );
    }
}
