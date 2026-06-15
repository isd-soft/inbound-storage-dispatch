package com.isd.wms.dto.process;

import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;

public interface ProcessSupervisorProjection {
    Long getReplenishmentId();
    Long getOrderId();
    TaskType getType();
    Long getStockId();
    String getProductName();
    String getLocationName();
    Status getStatus();
    Boolean getSourceLocationScanned();
    Boolean getProductScanned();
    Integer getPickedQuantity();
}
