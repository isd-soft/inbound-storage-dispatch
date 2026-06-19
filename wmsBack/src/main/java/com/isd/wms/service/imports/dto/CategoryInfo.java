package com.isd.wms.service.imports.dto;

import com.opencsv.bean.CsvBindByName;
import com.poiji.annotation.ExcelCellName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryInfo {
    @ExcelCellName(value = "Category", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Category", required = true)
    private String categoryName;
}
