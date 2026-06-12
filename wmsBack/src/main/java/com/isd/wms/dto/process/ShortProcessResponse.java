package com.isd.wms.dto.process;

public record ShortProcessResponse(
    Long id,
    String productName,
    String productBarcode,
    String locationName,
    String locationBarcode,
    Integer quantity) {
}
