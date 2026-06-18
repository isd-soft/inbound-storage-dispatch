package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

public record CategoryInfo(
    @ExcelCellName(value = "Category", mandatoryCell = true, mandatoryHeader = true)
    String categoryName
) {
}
