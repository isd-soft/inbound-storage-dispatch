package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;

public record OrderUpdateRequest(
        String logicId,
        OrderStatus status
) {
}
