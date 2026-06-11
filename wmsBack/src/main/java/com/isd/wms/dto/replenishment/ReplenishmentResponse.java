package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.Status;

import java.sql.Timestamp;

public record ReplenishmentResponse(
        Long id,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        Status status,
        Long destinationLocationId,
        Timestamp createdAt
) {
}
