package com.isd.wms.dto.order_line;

import com.isd.wms.enums.OrderStatus;

import java.sql.Timestamp;

public record OrderLineResponse(
        Long orderLineId,
        Long orderId,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        OrderStatus status,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
