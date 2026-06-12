package com.isd.wms.dto.order;

import lombok.NonNull;

public record OrderCreateRequest(
        @NonNull String logicId,
        @NonNull Long destinationLocationId
) {
}
