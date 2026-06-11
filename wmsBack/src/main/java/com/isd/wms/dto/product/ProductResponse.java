package com.isd.wms.dto.product;

import java.sql.Timestamp;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        Long categoryId,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
