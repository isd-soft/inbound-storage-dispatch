package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentStatus;


import java.sql.Timestamp;

public record ReplenishmentResponse(
        Long id,
        Long productId,
        Long taskId,
        Integer requestedQuantity,
        ReplenishmentStatus status,
        Long destinationLocationId,
        Timestamp createdAt
) {
}
