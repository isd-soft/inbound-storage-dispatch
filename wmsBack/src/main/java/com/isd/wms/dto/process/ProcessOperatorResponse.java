package com.isd.wms.dto.process;

public record ProcessOperatorResponse(
    Integer totalOfProcess,
    Integer currentIndexOfProcess,
    String orderLogicalId,
    String taskType,
    String destinationLocationBarcode,
    ShortProcessResponse processes) {
}
