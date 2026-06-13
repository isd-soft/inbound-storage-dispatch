package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.Status;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public record ReplenishmentResponse(
        Long id,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        Status status,
        Long destinationLocationId,
        LocalDateTime createdAt
) {
}
