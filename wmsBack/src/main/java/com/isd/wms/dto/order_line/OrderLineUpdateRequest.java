package com.isd.wms.dto.order_line;

import com.isd.wms.enums.OrderStatus;

public record OrderLineUpdateRequest(
        Long orderLineId,
        Long orderId,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        OrderStatus status,
        Long destinationLocationId
) {
}
