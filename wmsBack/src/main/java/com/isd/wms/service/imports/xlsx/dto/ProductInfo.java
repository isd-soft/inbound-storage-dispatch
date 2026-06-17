package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class ProductInfo {
    @ExcelCellName("Name")
    @NonNull
    private String name;

    @ExcelCellName("Barcode")
    @NonNull
    private String barcode;

    @ExcelCellName("Description")
    @NonNull
    private String description;

    @ExcelCellName("Category")
    @NonNull
    private String categoryName;

    public ProductInfo() {
    }
}
