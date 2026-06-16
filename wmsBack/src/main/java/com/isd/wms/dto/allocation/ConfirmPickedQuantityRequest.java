package com.isd.wms.dto.allocation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmPickedQuantityRequest(
        @NotNull @Min(1) Integer pickedQuantity
) {
}
