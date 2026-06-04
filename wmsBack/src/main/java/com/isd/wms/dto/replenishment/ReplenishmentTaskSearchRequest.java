package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentTaskStatus;

public record ReplenishmentTaskSearchRequest(
        Long productId,
        Long operatorId,
        Integer requestedQuantity,
        ReplenishmentTaskStatus status,
        Long sourceLocationId,
        Long destinationLocationId
) {
}
