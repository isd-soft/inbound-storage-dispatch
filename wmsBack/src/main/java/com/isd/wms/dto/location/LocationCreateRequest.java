package com.isd.wms.dto.location;

import com.isd.wms.enums.Zone;
import jakarta.validation.constraints.NotBlank;

public record LocationCreateRequest (
        String name,
        @NotBlank(message = "Barcode is required")
        String barcode,
        Zone zone,
        String description
) {}
