package com.isd.wms.dto.order;

import com.isd.wms.enums.OrderStatus;
import lombok.NonNull;

public record OrderUpdateRequest(
        @NonNull String logicId,
        @NonNull OrderStatus status,
        @NonNull Long destinationLocationId
) {
}
