package com.isd.wms.dto.order_line;

import com.isd.wms.enums.OrderStatus;
import jakarta.validation.constraints.Min;
import lombok.NonNull;

public record OrderLineUpdateRequest(
        @NonNull Long orderId,
        @NonNull Long taskId,
        @NonNull Long productId,
        @NonNull
        @Min(0)
        Integer requestedQuantity,
        @NonNull OrderStatus status,
        @NonNull Long destinationLocationId
) {
}
