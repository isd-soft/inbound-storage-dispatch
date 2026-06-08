package com.isd.wms.dto.order_line;

import com.isd.wms.enums.OrderStatus;

public record OrderLineCreateRequest(
        Long orderId,
        Long taskId,
        Long productId,
        Integer requestedQuantity,
        Long destinationLocationId
) {
}
