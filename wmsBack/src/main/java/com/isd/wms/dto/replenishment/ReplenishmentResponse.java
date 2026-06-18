package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.Status;

import java.time.LocalDateTime;

public record ReplenishmentResponse(
    Long id,
    Long taskId,
    Long productId,
    Integer requestedQuantity,
    Status status,
    Long destinationLocationId,
    Long assignedOperatorId,
    String transportUnitBarcode,
    LocalDateTime createdAt
) {
    public ReplenishmentResponse(
        Long id,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        Status status,
        Long destinationLocationId,
        LocalDateTime createdAt
    ) {
        this(id, taskId, productId, requestedQuantity, status, destinationLocationId, null, null, createdAt);
    }
}
