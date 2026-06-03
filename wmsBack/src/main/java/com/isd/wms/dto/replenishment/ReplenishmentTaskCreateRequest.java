package com.isd.wms.dto.replenishment;

public record ReplenishmentTaskCreateRequest(
        Long productId,
        Long operatorId,
        Long requestedQuantity,
        Long sourceLocationId,
        Long destinationLocationId
) {
}
