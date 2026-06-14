package com.isd.wms.dto.product;

public interface ProductWithQuantityProjection {
    Long getId();
    String getName();
    String getBarcode();
    Integer getQuantity();

    default String getSku() {
        return getBarcode();
    }
}
