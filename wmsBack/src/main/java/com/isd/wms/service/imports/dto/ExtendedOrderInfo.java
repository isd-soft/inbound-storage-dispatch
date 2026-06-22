package com.isd.wms.service.imports.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvRecurse;
import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelCellRange;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Import DTO for extended order data.
 * <p>
 * This DTO is structured to handle both order header information and a single
 * order line per row. It contains two inner classes: {@link OrderInfo} and
 * {@link OrderLineInfo}. The {@code @CsvRecurse} and {@code @ExcelCellRange}
 * annotations allow both parts to be read from the same flat file row.
 * </p>
 * <p>
 * For orders with multiple lines, the import service groups rows with the same
 * logic ID before creating the complete order.
 * </p>
 */
@Getter
@Setter
@ToString
public class ExtendedOrderInfo {
    @ExcelCellRange
    @CsvRecurse
    private OrderInfo orderInfo;

    @ExcelCellRange
    @CsvRecurse
    private OrderLineInfo orderLineInfo;

    @Getter
    @Setter
    @ToString
    public static class OrderInfo {
        @ExcelCellName(value = "Logic ID", mandatoryCell = true, mandatoryHeader = true)
        @CsvBindByName(column = "Logic ID", required = true)
        @NonNull
        @NotBlank
        private String logicId;

        @ExcelCellName(value = "Destination Location", mandatoryCell = true, mandatoryHeader = true)
        @CsvBindByName(column = "Destination Location", required = true)
        @NonNull
        @NotBlank
        private String destinationLocationName;
    }

    @Getter
    @Setter
    @ToString
    public static class OrderLineInfo {
        @ExcelCellName(value = "Product", mandatoryCell = true, mandatoryHeader = true)
        @CsvBindByName(column = "Product", required = true)
        @NonNull
        @NotBlank
        private String productName;

        @ExcelCellName(value = "Requested Quantity", mandatoryCell = true, mandatoryHeader = true)
        @CsvBindByName(column = "Requested Quantity", required = true)
        @NonNull
        private Integer requestedQuantity;
    }

    public ExtendedOrderInfo() {
    }
}
