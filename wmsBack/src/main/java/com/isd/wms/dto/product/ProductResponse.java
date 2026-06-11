package com.isd.wms.dto.product;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        Long categoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
