package com.isd.wms.dto.process;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfirmPickedQuantityRequest(
        @NotNull @Min(1) Integer pickedQuantity
) {
}
