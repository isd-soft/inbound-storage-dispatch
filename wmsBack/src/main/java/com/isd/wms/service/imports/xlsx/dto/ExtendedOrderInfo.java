package com.isd.wms.service.imports.xlsx.dto;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelCellRange;
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
        @ExcelCellName("Logic ID")
        private String logicId;

        @ExcelCellName("Destination Location")
        private String destinationLocationName;
    }

    @Getter
    @Setter
    public static class OrderLineInfo {
        @ExcelCellName("Product")
        private String productName;

        @ExcelCellName("Requested Quantity")
        private Integer requestedQuantity;
    }

    public ExtendedOrderInfo() {
    }
}
