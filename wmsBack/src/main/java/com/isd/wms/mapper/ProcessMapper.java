package com.isd.wms.mapper;

import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.dto.process.ShortProcessResponse;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessMapper {

    public ProcessOperatorResponse toOperatorResponse(Process process, Integer total, Integer current, String order) {
        return new ProcessOperatorResponse(
            total,
            current,
            order,
            toShortResponse(process)
        );
    }

    public ShortProcessResponse toShortResponse(Process process) {
        Stock stock = process.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new ShortProcessResponse(
            process.getId(),
            product.getName(),
            product.getBarcode(),
            stock.getLocation().getBarcode(),
            stock.getLocation().getBarcode(),
            process.getQuantity()
        );
    }

    public ProcessResponse toResponse(Process process) {
        Stock stock = process.getStock();
        Product product = stock.getProduct()
            .orElseThrow(() -> new IllegalStateException("Stock product is required"));
        return new ProcessResponse(
            process.getId(),
            product.getName(),
            product.getBarcode(),
            stock.getLocation().getBarcode(),
            process.getQuantity(),
            process.getStatus()
        );
    }
}
