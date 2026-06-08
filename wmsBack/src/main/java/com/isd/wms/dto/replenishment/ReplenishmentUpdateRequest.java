package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentStatus;

public record ReplenishmentUpdateRequest(
        Long productId,
        Integer requestedQuantity,
        ReplenishmentStatus status,
        Long destinationLocationId
) {
}
