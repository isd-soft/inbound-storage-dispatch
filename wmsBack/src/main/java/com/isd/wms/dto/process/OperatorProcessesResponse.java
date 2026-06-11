package com.isd.wms.dto.process;

import java.util.List;

public record OperatorProcessesResponse(
    Integer processesLeft,
    List<ProcessResponse> processes
) {
}
