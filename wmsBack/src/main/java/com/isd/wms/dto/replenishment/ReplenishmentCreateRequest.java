package com.isd.wms.dto.replenishment;

public record ReplenishmentCreateRequest(
        Long productId,
        Integer requestedQuantity,
        Long destinationLocationId
) {
}
