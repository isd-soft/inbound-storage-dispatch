package com.isd.wms.mapper;

import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import org.springframework.stereotype.Component;

@Component
public class StockMapper {

    public StockResponse toResponse(Stock stock) {
        Product product = stock.getProduct();
        Location location = stock.getLocation();
        return StockResponse.builder()
                .id(stock.getId())
                .sku(stock.getSku())
                .productId(product == null ? null : product.getId())
                .productName(product == null ? null : product.getName())
                .locationId(location == null ? null : location.getId())
                .locationCode(location == null ? null : location.getLocationCode())
                .quantity(stock.getQuantity())
                .manufactureDate(stock.getManufactureDate())
                .expirationDate(stock.getExpirationDate())
                .build();
    }
}
