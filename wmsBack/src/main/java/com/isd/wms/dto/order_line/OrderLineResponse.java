package com.isd.wms.dto.order_line;

import com.isd.wms.enums.Status;

import java.sql.Timestamp;

public record OrderLineResponse(
        Long orderLineId,
        Long orderId,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        Status status,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
