package com.isd.wms.dto.process;

import com.isd.wms.enums.Status;

public record ProcessResponse(
        Long id,
        String productName,
        String barcode,
        String locationBarcode,
        Integer quantity,
        Status status) {
}
