package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

import java.time.LocalDate;

public record StockInfo(
    @ExcelCellName(value = "Product", mandatoryCell = true, mandatoryHeader = true)
    @NonNull
    @NotBlank
    String productName,

    @ExcelCellName(value = "Location", mandatoryCell = true, mandatoryHeader = true)
    @NonNull
    @NotBlank
    String locationName,

    @ExcelCellName(value = "Quantity", mandatoryCell = true, mandatoryHeader = true)
    @NonNull
    Integer quantity,

    @ExcelCellName(value = "Reserved Quantity", mandatoryCell = true, mandatoryHeader = true)
    @NonNull
    Integer reservedQuantity,

    @ExcelCellName(value = "Manufacture Date", mandatoryCell = true, mandatoryHeader = true)
    @NonNull
    LocalDate manufactureDate,

    @ExcelCellName(value = "Expiration Date", mandatoryCell = true, mandatoryHeader = true)
    @NonNull
    LocalDate expirationDate
) {
}
