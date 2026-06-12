package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;

import java.sql.Timestamp;

public record OrderResponse(
        Long id,
        String logicId,
        Long destinationLocationId,
        OrderStatus Status,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
