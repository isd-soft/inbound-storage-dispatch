package com.isd.wms.dto.location;

import com.isd.wms.enums.Zone;
import jakarta.validation.constraints.NotBlank;

public record LocationCreateRequest (
        @NotBlank(message = "Location code is required")
        String locationCode,
        Zone zone,
        String description
) {}
