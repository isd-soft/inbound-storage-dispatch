package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelCellRange;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class ExtendedOrderInfo {
    @ExcelCellRange
    private OrderInfo orderInfo;

    @ExcelCellRange
    private OrderLineInfo orderLineInfo;

    @Getter
    @Setter
    public static class OrderInfo {
        @ExcelCellName(value = "Logic ID", mandatoryCell = true, mandatoryHeader = true)
        @NonNull
        @NotBlank
        private String logicId;

        @ExcelCellName(value = "Destination Location", mandatoryCell = true, mandatoryHeader = true)
        @NonNull
        @NotBlank
        private String destinationLocationName;
    }

    @Getter
    @Setter
    public static class OrderLineInfo {
        @ExcelCellName(value = "Product", mandatoryCell = true, mandatoryHeader = true)
        @NonNull
        @NotBlank
        private String productName;

        @ExcelCellName(value = "Requested Quantity", mandatoryCell = true, mandatoryHeader = true)
        @NonNull
        private Integer requestedQuantity;
    }

    public ExtendedOrderInfo() {
    }
}
