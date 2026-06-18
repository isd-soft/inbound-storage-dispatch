package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;

import java.time.LocalDate;

public record StockInfo(
    @ExcelCellName(value = "Product", mandatoryCell = true, mandatoryHeader = true) String productName,
    @ExcelCellName(value = "Location", mandatoryCell = true, mandatoryHeader = true) String locationName,
    @ExcelCellName(value = "Quantity", mandatoryCell = true, mandatoryHeader = true) Integer quantity,
    @ExcelCellName(value = "Reserved Quantity", mandatoryCell = true, mandatoryHeader = true) Integer reservedQuantity,
    @ExcelCellName(value = "Manufacture Date", mandatoryCell = true, mandatoryHeader = true) LocalDate manufactureDate,
    @ExcelCellName(value = "Expiration Date", mandatoryCell = true, mandatoryHeader = true) LocalDate expirationDate
) {
}
