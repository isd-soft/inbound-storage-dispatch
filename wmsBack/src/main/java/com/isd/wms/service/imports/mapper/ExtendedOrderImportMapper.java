package com.isd.wms.service.imports.mapper;

import com.isd.wms.dto.order.ExtendedOrderCreateRequest;
import com.isd.wms.dto.order.OrderCreateRequest;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.xlsx.dto.ExtendedOrderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExtendedOrderImportMapper implements ImportMapper<ExtendedOrderInfo, ExtendedOrderCreateRequest> {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    public ExtendedOrderCreateRequest toEntity(ExtendedOrderInfo info) {
        System.out.println(info);
        return new ExtendedOrderCreateRequest(
            new OrderCreateRequest(
                info.getOrderInfo().getLogicId(),
                getLocationId(info.getOrderInfo().getDestinationLocationName())
            ),
            List.of(new OrderLineCreateRequest(
                null,
                getProductId(info.getOrderLineInfo().getProductName()),
                info.getOrderLineInfo().getRequestedQuantity()
            ))
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
    public Class<ExtendedOrderInfo> supports() {
        return ExtendedOrderInfo.class;
    }
}
