package com.isd.wms.dto.process;

public record ProcessOperatorResponse(
    Integer totalOfProcess,
    Integer currentIndexOfProcess,
    String orderLogicalId,
    ShortProcessResponse processes) {
}
