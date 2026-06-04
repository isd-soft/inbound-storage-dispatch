package com.isd.wms.dto.replenishment;

public record ReplenishmentTaskCreateRequest(
        Long productId,
        Integer requestedQuantity,
        Long sourceLocationId,
        Long destinationLocationId
) {
}
