package com.isd.wms.dto.order;

import com.isd.wms.enums.Status;

import java.sql.Timestamp;

public record OrderSearchRequest(
        Long orderId,
        String logicId,
        Long destinationLocationId,
        Status status,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
