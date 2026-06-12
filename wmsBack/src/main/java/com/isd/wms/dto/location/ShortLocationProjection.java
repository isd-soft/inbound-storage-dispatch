package com.isd.wms.dto.location;

public interface ShortLocationProjection {
    Long getId();
    String getBarcode();

    default String getLocationCode() {
        return getBarcode();
    }
}
