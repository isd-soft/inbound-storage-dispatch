package com.isd.wms.dto.allocation;

import com.isd.wms.enums.Status;

public record AllocationResponse(
        Long id,
        String productName,
        String barcode,
        String locationBarcode,
        Integer quantity,
        Status status) {
}
