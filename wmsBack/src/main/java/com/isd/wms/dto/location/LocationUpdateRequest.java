package com.isd.wms.dto.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest (
        @NotBlank(message = "Location code is required") String locationCode,
        String zone,
        String description,
        @NotNull(message = "Availability status is required") Boolean available
) {}
