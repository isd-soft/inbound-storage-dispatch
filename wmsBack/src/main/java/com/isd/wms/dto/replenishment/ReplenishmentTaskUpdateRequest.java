package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentTaskStatus;

public record ReplenishmentTaskUpdateRequest(
        Long replenishmentTaskId,
        Long productId,
        Long operatorId,
        Long requestedQuantity,
        Long sourceLocationId,
        Long destinationLocationId,
        ReplenishmentTaskStatus status
) {
}
