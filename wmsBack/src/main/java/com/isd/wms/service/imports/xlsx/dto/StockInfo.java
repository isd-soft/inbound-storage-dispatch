package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

import java.time.LocalDate;

public record StockInfo(
    @ExcelCellName("Product") String productName,
    @ExcelCellName("Location") String locationName,
    @ExcelCellName("Quantity") Integer quantity,
    @ExcelCellName("Reserved Quantity") Integer reservedQuantity,
    @ExcelCellName("Manufacture Date") LocalDate manufactureDate,
    @ExcelCellName("Expiration Date") LocalDate expirationDate
) {
}
