package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;

import java.sql.Timestamp;

public record OrderSearchRequest(
        Long orderId,
        String logicId,
        Long destinationLocationId,
        OrderStatus status,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
