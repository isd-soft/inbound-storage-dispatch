package com.isd.wms.service.imports.mapper;

import com.isd.wms.dto.order.ExtendedOrderCreateRequest;
import com.isd.wms.dto.order.OrderCreateRequest;
import com.isd.wms.dto.order_line.OrderLineCreateRequest;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.dto.ExtendedOrderInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting {@link ExtendedOrderInfo} DTOs to
 * {@link ExtendedOrderCreateRequest} objects.
 * <p>
 * Resolves product and destination location by name. Each row produces an
 * order with a single line. The import service later groups rows with the
 * same logic ID to create multi‑line orders.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ExtendedOrderImportMapper implements ImportMapper<ExtendedOrderInfo, ExtendedOrderCreateRequest> {

    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    @Override
    public ExtendedOrderCreateRequest toEntity(ExtendedOrderInfo info) {
        try {
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
        } catch (Exception e) {
            System.out.println(getProductId(info.getOrderLineInfo().getProductName()));
            System.out.println(getLocationId(info.getOrderInfo().getDestinationLocationName()));
            throw new InvalidRequestException(String.format("An error occurred at parsing the order %s.", info.getOrderInfo().getLogicId()));
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
    public Class<ExtendedOrderInfo> supports() {
        return ExtendedOrderInfo.class;
    }
}
