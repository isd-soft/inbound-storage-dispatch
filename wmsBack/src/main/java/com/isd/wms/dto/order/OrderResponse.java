package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.ReplenishmentStatus;

import java.sql.Timestamp;

public record OrderResponse(
        Long id,
        String logicId,
        OrderStatus Status
) {
}
