package com.isd.wms.service.imports.mapper;

import com.isd.wms.entity.Location;
import com.isd.wms.service.imports.dto.LocationInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationImportMapper implements ImportMapper<LocationInfo, Location> {

    @Override
    public Location toEntity(LocationInfo info) {
        return new Location(
            info.getName(),
            info.getBarcode(),
            info.getZone(),
            info.getDescription()
        );
    }

    @Override
    public Class<LocationInfo> supports() {
        return LocationInfo.class;
    }
}
