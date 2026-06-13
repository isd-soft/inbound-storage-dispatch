package com.isd.wms.repository.projections;

public interface OperatorProcessProjection {
    Long getOldestOrderId();
    String getOrderName();
    Long getProcessId();
    String getProductName();
    String getProductBarcode();
    String getLocationName();
    String getLocationBarcode();
    Integer getQuantity();
}
