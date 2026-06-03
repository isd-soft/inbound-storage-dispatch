package com.isd.wms.dto.location;

import jakarta.validation.constraints.NotBlank;

public record LocationCreateRequest (
        @NotBlank(message = "Location code is required")
        String locationCode,
        String zone,
        String description
) {}
