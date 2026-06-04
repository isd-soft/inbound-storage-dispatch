package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentTaskStatus;

import java.sql.Timestamp;

public record ReplenishmentTaskResponse(
        Long id,
        Long productId,
        Long operatorId,
        Integer requestedQuantity,
        ReplenishmentTaskStatus status,
        Long sourceLocationId,
        Long destinationLocationId,
        Timestamp createdAt
) {
}
