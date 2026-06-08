package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;

public record OrderUpdateRequest(
        Long orderId,
        String logicId,
        OrderStatus status
) {
}
