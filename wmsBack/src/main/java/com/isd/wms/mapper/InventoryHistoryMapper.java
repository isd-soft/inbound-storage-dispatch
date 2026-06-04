package com.isd.wms.mapper;

import com.isd.wms.dto.inventory.InventoryHistoryResponse;
import com.isd.wms.entity.InventoryHistory;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class InventoryHistoryMapper {

    public InventoryHistoryResponse toResponse(InventoryHistory history) {
        Product product = history.getProduct();
        Location sourceLocation = history.getSourceLocation();
        Location destinationLocation = history.getDestinationLocation();
        User user = history.getUser();
        return InventoryHistoryResponse.builder()
                .id(history.getId())
                .productId(product == null ? null : product.getId())
                .productName(product == null ? null : product.getName())
                .sku(history.getSku())
                .alteredQuantity(history.getAlteredQuantity())
                .quantityAfterChange(history.getQuantityAfterChange())
                .sourceLocationId(sourceLocation == null ? null : sourceLocation.getId())
                .sourceLocationCode(sourceLocation == null ? null : sourceLocation.getLocationCode())
                .destinationLocationId(destinationLocation == null ? null : destinationLocation.getId())
                .destinationLocationCode(destinationLocation == null ? null : destinationLocation.getLocationCode())
                .operationType(history.getOperationType().name())
                .timestamp(history.getTimestamp())
                .userId(user == null ? null : user.getId())
                .username(user == null ? null : user.getUsername())
                .build();
    }
}
