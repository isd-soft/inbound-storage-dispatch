package com.isd.wms.dto.order;

import com.isd.wms.enums.Status;
import lombok.NonNull;

public record OrderUpdateRequest(
        @NonNull String logicId,
        @NonNull Long destinationLocationId,
        @NonNull Status status
) {
}
