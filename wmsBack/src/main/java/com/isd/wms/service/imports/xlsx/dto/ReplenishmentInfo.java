package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

public record ReplenishmentInfo(
    @ExcelCellName("Product") String product,
    @ExcelCellName("Requested Quantity") Integer requestedQuantity,
    @ExcelCellName("Destination Location") String destinationLocationName
) {
}
