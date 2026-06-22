package com.isd.wms.service.imports.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import com.poiji.annotation.ExcelCellName;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

/**
 * Import DTO for stock data.
 * <p>
 * Represents a stock record with product name, location name, quantities,
 * and manufacturing/expiration dates. Dates are expected in the format
 * {@code dd.MM.yyyy}.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockInfo {
    @ExcelCellName(value = "Product", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Product", required = true)
    @NonNull
    @NotBlank
    private String productName;

    @ExcelCellName(value = "Location", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Location", required = true)
    @NonNull
    @NotBlank
    private String locationName;

    @ExcelCellName(value = "Quantity", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Quantity", required = true)
    @NonNull
    private Integer quantity;

    @ExcelCellName(value = "Reserved Quantity", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Reserved Quantity", required = true)
    @NonNull
    private Integer reservedQuantity;

    @ExcelCellName(value = "Manufacture Date", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Manufacture Date", required = true)
    @CsvDate("dd.MM.yyyy")
    @NonNull
    private LocalDate manufactureDate;

    @ExcelCellName(value = "Expiration Date", mandatoryCell = true, mandatoryHeader = true)
    @CsvBindByName(column = "Expiration Date", required = true)
    @CsvDate("dd.MM.yyyy")
    @NonNull
    private LocalDate expirationDate;
}
