package com.isd.wms.service.ai;

import com.isd.wms.dto.ai.StockCheckRequest;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("wmsAiTools")
@RequiredArgsConstructor
public class WmsAiTools {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Tool(description = "Use this to check the available stock of a product in the warehouse by its barcode. Returns the quantity and locations.")
    public String checkStockByBarcode(StockCheckRequest request) {
        log.info("AI invoked checkStockByBarcode tool for barcode: {}", request.barcode());

        Product product = productRepository.findByBarcode(request.barcode()).orElse(null);

        if (product == null) {
            return "Product with barcode " + request.barcode() + " not found in the database.";
        }

        List<Stock> stocks = stockRepository.findAvailableStocksByProductId(product.getId());

        if (stocks.isEmpty()) {
            return "Product '" + product.getName() + "' found, but it is currently out of stock (quantity 0).";
        }

        StringBuilder response = new StringBuilder();
        response.append("Product: '").append(product.getName()).append("'.\n");
        response.append("Found in the following locations:\n");

        int totalAvailable = 0;
        for (Stock stock : stocks) {
            int available = stock.getQuantity() - stock.getReservedQuantity();
            if (available > 0) {
                response.append("- Location ").append(stock.getLocation().getBarcode())
                    .append(": available ").append(available).append(" pcs.\n");
                totalAvailable += available;
            }
        }

        response.append("Total available stock: ").append(totalAvailable).append(" pcs.");
        return response.toString();
    }
}
