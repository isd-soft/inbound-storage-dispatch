package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

public record ProductInfo(
    @ExcelCellName("Name") String name,
    @ExcelCellName("Barcode") String barcode,
    @ExcelCellName("Description") String description,
    @ExcelCellName("Category") String categoryName
) {
}
