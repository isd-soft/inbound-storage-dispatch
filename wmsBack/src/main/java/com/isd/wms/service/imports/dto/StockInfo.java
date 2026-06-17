package com.isd.wms.service.imports.dto;

import com.poiji.annotation.ExcelCellName;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StockInfo {
    @ExcelCellName("Product")
    @NonNull
    private Long productId;

    @ExcelCellName("Location")
    @NonNull
    private Long locationId;

    @ExcelCellName("Quantity")
    @NonNull
    private Integer quantity;

    @ExcelCellName("Reserved Quantity")
    @NonNull
    private Integer reservedQuantity;

    @ExcelCellName("Manufacture Date")
    private LocalDate manufactureDate;

    @ExcelCellName("Expiration Date")
    private LocalDate expirationDate;

    public StockInfo() {
    }
}
