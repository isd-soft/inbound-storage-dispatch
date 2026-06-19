package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

public record ProductInfo(
    @ExcelCellName(value = "Name", mandatoryCell = true, mandatoryHeader = true) String name,
    @ExcelCellName(value = "Barcode", mandatoryCell = true, mandatoryHeader = true) String barcode,
    @ExcelCellName(value = "Description", mandatoryCell = true, mandatoryHeader = true) String description,
    @ExcelCellName(value = "Category", mandatoryCell = true, mandatoryHeader = true) String categoryName
) {
}
