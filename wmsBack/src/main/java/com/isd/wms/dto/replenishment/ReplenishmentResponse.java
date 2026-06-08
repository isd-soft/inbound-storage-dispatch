package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentStatus;

public record ReplenishmentResponse(
        Long id,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        ReplenishmentStatus status,
        Long destinationLocationId
) {
}
