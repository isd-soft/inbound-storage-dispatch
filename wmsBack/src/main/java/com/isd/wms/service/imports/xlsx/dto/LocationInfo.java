package com.isd.wms.service.imports.xlsx.dto;

import com.isd.wms.enums.Zone;
import com.poiji.annotation.ExcelCellName;

public record LocationInfo(
    @ExcelCellName(value = "Name", mandatoryCell = true, mandatoryHeader = true) String name,
    @ExcelCellName(value = "Barcode", mandatoryCell = true, mandatoryHeader = true) String barcode,
    @ExcelCellName(value = "Description", mandatoryCell = true, mandatoryHeader = true) String description,
    @ExcelCellName(value = "Zone", mandatoryCell = true, mandatoryHeader = true) Zone zone
) {
}
