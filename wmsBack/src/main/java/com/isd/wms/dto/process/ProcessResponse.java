package com.isd.wms.dto.process;

import com.isd.wms.enums.ProcessStatus;

public record ProcessResponse(
        Long id,
        Long taskId,
        Long productId,
        String productName,
        String locationCode,
        Integer quantity,
        ProcessStatus status
) {
}