package com.isd.wms.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String barcode,
        @Size(max = 255) String description,
        @NotNull @Min(1) Long categoryId,
        Boolean autoReplenish,
        @Min(0) Integer minThreshold,
        @Min(1) Integer replenishQty
) {
}
