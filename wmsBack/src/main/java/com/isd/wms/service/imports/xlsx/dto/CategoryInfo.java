package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

public record CategoryInfo(
    @ExcelCellName("Category")
    String categoryName
) {
}
