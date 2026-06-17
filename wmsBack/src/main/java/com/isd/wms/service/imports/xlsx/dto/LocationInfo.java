package com.isd.wms.service.imports.xlsx.dto;

import com.isd.wms.enums.Zone;
import com.poiji.annotation.ExcelCellName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class LocationInfo {
    @ExcelCellName("Name")
    @NonNull
    private String name;

    @ExcelCellName("Barcode")
    @NonNull
    private String barcode;

    @ExcelCellName("Description")
    @NonNull
    private String description;

    @ExcelCellName("Zone")
    @NonNull
    private Zone zone;

    public LocationInfo() {
    }
}
