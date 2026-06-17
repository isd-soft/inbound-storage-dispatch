package com.isd.wms.service.imports.mapper;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.xlsx.dto.StockInfo;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockImportMapper implements ImportMapper<StockInfo, AddStockRequest> {

    private final SecurityFacade securityFacade;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    public AddStockRequest toEntity(StockInfo info) {
        return new AddStockRequest(
            getProductId(info.getProductName()),
            getLocationId(info.getLocationName()),
            info.getQuantity(),
            info.getReservedQuantity(),
            info.getManufactureDate(),
            info.getExpirationDate(),
            securityFacade.getCurrentUser().getId()
        );
    }

    private Long getProductId(String name) {
        return productRepository.findProductIdByName(name)
            .orElseThrow(() -> new ProductNotFoundException(name));
    }

    private Long getLocationId(String name) {
        return locationRepository.findLocationIdByName(name)
            .orElseThrow(() -> new LocationNotFoundException(name));
    }

    @Override
    public Class<StockInfo> supports() {
        return StockInfo.class;
    }
}
