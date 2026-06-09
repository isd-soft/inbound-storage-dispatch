package com.isd.wms.dto.order_line;

import jakarta.validation.constraints.Min;
import lombok.NonNull;

public record OrderLineCreateRequest(
        @NonNull Long orderId,
        @NonNull Long taskId,
        @NonNull Long productId,
        @NonNull
        @Min(0)
        Integer requestedQuantity,
        @NonNull Long destinationLocationId
) {
}
