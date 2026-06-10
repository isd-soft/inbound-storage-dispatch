package com.isd.wms.dto.order_line;

import com.isd.wms.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineUpdateRequest(
        @NotNull Long orderId,
        @NotNull Long taskId,
        @NotNull Long productId,
        @Positive(message = "Replenishment requested quantity cannot be nonpositive") Integer requestedQuantity,
        @NotNull OrderStatus status,
        @NotNull Long destinationLocationId
) {}
