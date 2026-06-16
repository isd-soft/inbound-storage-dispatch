package com.isd.wms.service.imports.mapper;

import com.isd.wms.entity.Stock;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.xlsx.StockInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockImportMapper implements ImportMapper<StockInfo, Stock> {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    public Stock toEntity(StockInfo info) {
        return new Stock(
            productRepository.findById(info.getProductId())
                .orElseThrow(),
            locationRepository.findById(info.getLocationId())
                .orElseThrow(),
            info.getQuantity(),
            info.getReservedQuantity(),
            info.getManufactureDate(),
            info.getExpirationDate()
        );
    }

    @Override
    public Class<StockInfo> supports() {
        return StockInfo.class;
    }
}
