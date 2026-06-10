package com.isd.wms.dto.order_line;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineCreateRequest(
        @NotNull Long orderId,
        @NotNull Long taskId,
        @NotNull Long productId,
        @Positive(message = "Replenishment requested quantity cannot be nonpositive") Integer requestedQuantity,
        @NotNull Long destinationLocationId
) {
}
