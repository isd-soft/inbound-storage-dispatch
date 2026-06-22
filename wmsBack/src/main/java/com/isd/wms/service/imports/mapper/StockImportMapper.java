package com.isd.wms.service.imports.mapper;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.dto.StockInfo;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting {@link StockInfo} DTOs to {@link AddStockRequest} objects.
 * <p>
 * Resolves product and location by name and uses the current authenticated user
 * (via {@link SecurityFacade}) as the user performing the stock addition.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class StockImportMapper implements ImportMapper<StockInfo, AddStockRequest> {

    private final SecurityFacade securityFacade;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    public AddStockRequest toEntity(StockInfo info) {
        try {
            return new AddStockRequest(
                getProductId(info.getProductName()),
                getLocationId(info.getLocationName()),
                info.getQuantity(),
                info.getReservedQuantity(),
                info.getManufactureDate(),
                info.getExpirationDate(),
                securityFacade.getCurrentUser().getId()
            );
        } catch (Exception e) {
            throw new InvalidRequestException(
                String.format("An error occurred at parsing the stock of product %s at location %s.",
                    info.getProductName(),
                    info.getLocationName()
                    ));
        }
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
