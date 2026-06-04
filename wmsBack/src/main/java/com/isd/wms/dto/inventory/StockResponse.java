package com.isd.wms.dto.inventory;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockResponse {
    private Long id;
    private String sku;
    private Long productId;
    private String productName;
    private Long locationId;
    private String locationCode;
    private Integer quantity;
    private LocalDate manufactureDate;
    private LocalDate expirationDate;
}
