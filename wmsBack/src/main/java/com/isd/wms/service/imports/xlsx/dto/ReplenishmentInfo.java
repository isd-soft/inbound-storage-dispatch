package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

public record ReplenishmentInfo(
    @ExcelCellName(value = "Product", mandatoryCell = true, mandatoryHeader = true) String product,
    @ExcelCellName(value = "Requested Quantity", mandatoryCell = true, mandatoryHeader = true) Integer requestedQuantity,
    @ExcelCellName(value = "Destination Location", mandatoryCell = true, mandatoryHeader = true) String destinationLocationName
) {
}
