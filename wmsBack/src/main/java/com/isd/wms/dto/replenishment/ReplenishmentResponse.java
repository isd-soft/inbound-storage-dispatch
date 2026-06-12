package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentStatus;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public record ReplenishmentResponse(
        Long id,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        ReplenishmentStatus status,
        Long destinationLocationId,
        LocalDateTime createdAt
) {
}
