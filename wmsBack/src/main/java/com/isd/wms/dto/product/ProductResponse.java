package com.isd.wms.dto.product;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName
) {
}
