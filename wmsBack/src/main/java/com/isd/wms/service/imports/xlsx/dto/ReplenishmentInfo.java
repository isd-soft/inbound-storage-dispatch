package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class ReplenishmentInfo {
    @ExcelCellName("Product")
    @NonNull
    private String product;

    @ExcelCellName("Requested Quantity")
    @NonNull
    private Integer requestedQuantity;

    @ExcelCellName("Destination Location")
    @NonNull
    private String destinationLocationName;

    public ReplenishmentInfo() {
    }
}
