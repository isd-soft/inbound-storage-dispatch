package com.isd.wms.service.imports.mapper;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.entity.Category;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.repository.CategoryRepository;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.xlsx.dto.ProductInfo;
import com.isd.wms.service.imports.xlsx.dto.ReplenishmentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplenishmentImportMapper implements ImportMapper<ReplenishmentInfo, ReplenishmentCreateRequest> {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    public ReplenishmentCreateRequest toEntity(ReplenishmentInfo info) {
    return new ReplenishmentCreateRequest(
            getProductId(info.getProduct()),
            info.getRequestedQuantity(),
            getLocationId(info.getDestinationLocationName())
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
    public Class<ReplenishmentInfo> supports() {
        return ReplenishmentInfo.class;
    }
}
