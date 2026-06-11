package com.isd.wms.mapper;

import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import org.springframework.stereotype.Component;

@Component
public class ProcessMapper {

    public ProcessOperatorResponse toOperatorResponse(Process process, Integer left) {
        Stock stock = process.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new ProcessOperatorResponse(
            left,
            process.getId(),
            product.getName(),
            product.getSku(),
            stock.getLocation().getLocationCode(),
            process.getQuantity(),
            process.getStatus()
        );
    }

    public ProcessResponse toResponse(Process process) {
        Stock stock = process.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new ProcessResponse(
            process.getId(),
            product.getName(),
            product.getSku(),
            stock.getLocation().getLocationCode(),
            process.getQuantity(),
            process.getStatus()
        );
    }
}
