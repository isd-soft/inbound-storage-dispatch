package com.isd.wms.dto.replenishment;

import com.isd.wms.enums.ReplenishmentStatus;
import lombok.NonNull;

public record ReplenishmentUpdateRequest(
        @NonNull Long taskId,
        @NonNull Long productId,
        @NonNull Integer requestedQuantity,
        @NonNull ReplenishmentStatus status,
        @NonNull Long destinationLocationId
) {
}
