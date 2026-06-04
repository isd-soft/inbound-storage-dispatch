package com.isd.wms.dto.replenishment;

public record ReplenishmentTaskCreateRequest(
        Long productId,
        Long requestedQuantity,
        Long sourceLocationId,
        Long destinationLocationId
) {
}
