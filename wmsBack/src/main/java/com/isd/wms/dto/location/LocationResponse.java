package com.isd.wms.dto.location;

import com.isd.wms.enums.Zone;

public record LocationResponse(
        Long id,
        String locationCode,
        Zone zone,
        String description,
        Boolean available
) {}
