package com.isd.wms.dto.allocation;

import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;

public record AllocationSupervisorResponse(
    Long allocationId,
    Long replenishmentId,
    String replenishmentLogicId,
    Long orderId,
    String orderLogicId,
    TaskType type,
    Long stockId,
    String productName,
    String locationName,
    Integer requestedQuantity,
    Integer deliveredQuantity,
    Status status,
    Boolean sourceLocationScanned,
    Boolean productScanned
) {
}
