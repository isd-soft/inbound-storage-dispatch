package com.isd.wms.dto.location;

public record LocationResponse(
        Long id,
        String locationCode,
        String zone,
        String description,
        Boolean available
) {}
