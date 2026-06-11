package com.isd.wms.mapper;

import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import org.springframework.stereotype.Component;

@Component
public class ProcessMapper {

    public ProcessOperatorResponse toResponse(Process process) {
        Stock stock = process.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new ProcessOperatorResponse(
            process.getId(),
            product.getName(),
            product.getSku(),
            stock.getLocation().getLocationCode(),
            process.getQuantity(),
            process.getStatus()
        );
    }
}
