package com.isd.wms.dto.process;

public record ProcessExecutionResponse(
        Long processId,
        String status,
        boolean sourceLocationScanned,
        boolean productScanned,
        Integer requiredQuantity,
        Integer pickedQuantity
) {
}
