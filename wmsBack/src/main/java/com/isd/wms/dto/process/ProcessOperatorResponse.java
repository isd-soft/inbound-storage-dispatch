package com.isd.wms.dto.process;

import com.isd.wms.enums.Status;

public record ProcessOperatorResponse (
        Long id,
        String productName,
        String Sku,
        String locationCode,
        Integer quantity,
        Status status) {
}
