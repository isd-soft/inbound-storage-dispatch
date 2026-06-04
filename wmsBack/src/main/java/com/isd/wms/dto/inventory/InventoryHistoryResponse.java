package com.isd.wms.dto.inventory;

import java.time.Instant;
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
public class InventoryHistoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Integer alteredQuantity;
    private Integer quantityAfterChange;
    private Long sourceLocationId;
    private String sourceLocationCode;
    private Long destinationLocationId;
    private String destinationLocationCode;
    private String operationType;
    private Instant timestamp;
    private Long userId;
    private String username;
}
