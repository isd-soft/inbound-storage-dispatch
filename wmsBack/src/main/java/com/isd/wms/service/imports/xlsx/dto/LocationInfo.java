package com.isd.wms.service.imports.xlsx.dto;

import com.isd.wms.enums.Zone;
import com.poiji.annotation.ExcelCellName;

public record LocationInfo(
    @ExcelCellName("Name") String name,
    @ExcelCellName("Barcode") String barcode,
    @ExcelCellName("Description") String description,
    @ExcelCellName("Zone") Zone zone
) {
}
