package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String logicId,
        Long destinationLocationId,
        OrderStatus Status,
        Long assignedOperatorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public OrderResponse(
            Long id,
            String logicId,
            Long destinationLocationId,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, logicId, destinationLocationId, status, null, createdAt, updatedAt);
    }

    public OrderStatus status() {
        return Status;
    }
}
