package com.isd.wms.dto.replenishment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReplenishmentCreateRequest(
        @NotNull(message = "Product ID is mandatory") Long productId,
        @Positive(message = "Requested quantity must be positive") Integer requestedQuantity,
        @NotNull(message = "Destination location ID is mandatory") Long destinationLocationId
) {}