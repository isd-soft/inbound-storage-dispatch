package com.isd.wms.service.imports.xlsx;

import com.poiji.annotation.ExcelCellName;
import jakarta.validation.constraints.Min;
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
    @Min(0)
    private Integer quantity;

    @ExcelCellName("Reserved Quantity")
    @NonNull
    @Min(0)
    private Integer reservedQuantity;

    @ExcelCellName("Manufacture Date")
    private LocalDate manufactureDate;

    @ExcelCellName("Expiration Date")
    private LocalDate expirationDate;

    public StockInfo() {
    }

    public StockInfo(Long productId, Long locationId, Integer quantity, LocalDate manufactureDate, LocalDate expirationDate) {
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.manufactureDate = manufactureDate;
        this.expirationDate = expirationDate;
    }

    public StockInfo(Long productId, Long locationId, Integer quantity, Integer reservedQuantity, LocalDate manufactureDate, LocalDate expirationDate) {
        this.productId = productId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.manufactureDate = manufactureDate;
        this.expirationDate = expirationDate;
    }
}
