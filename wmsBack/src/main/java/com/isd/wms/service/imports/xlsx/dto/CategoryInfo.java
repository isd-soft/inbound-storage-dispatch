package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class CategoryInfo {
    @ExcelCellName("Category")
    @NonNull
    private String categoryName;

    public CategoryInfo() {
    }
}
