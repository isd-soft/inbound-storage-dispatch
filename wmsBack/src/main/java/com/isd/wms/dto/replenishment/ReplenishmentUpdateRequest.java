package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentStatus;
import jakarta.validation.constraints.Min;
import lombok.NonNull;

public record ReplenishmentUpdateRequest(
        @NonNull Long taskId,
        @NonNull Long productId,
        @NonNull @Min(0) Integer requestedQuantity,
        @NonNull ReplenishmentStatus status,
        @NonNull Long destinationLocationId
) {
}
