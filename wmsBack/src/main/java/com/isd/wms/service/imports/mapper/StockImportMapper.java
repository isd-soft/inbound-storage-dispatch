package com.isd.wms.service.imports.mapper;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.service.imports.dto.StockInfo;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockImportMapper implements ImportMapper<StockInfo, AddStockRequest> {

    private final SecurityFacade securityFacade;

    @Override
    public AddStockRequest toEntity(StockInfo info) {
        return new AddStockRequest(
            info.getProductId(),
            info.getLocationId(),
            info.getQuantity(),
            info.getReservedQuantity(),
            info.getManufactureDate(),
            info.getExpirationDate(),
            securityFacade.getCurrentUser().getId()
        );
    }

    @Override
    public Class<StockInfo> supports() {
        return StockInfo.class;
    }
}
