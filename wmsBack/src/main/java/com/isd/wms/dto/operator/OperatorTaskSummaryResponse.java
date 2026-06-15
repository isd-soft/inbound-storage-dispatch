package com.isd.wms.dto.operator;

import com.isd.wms.enums.OrderStatus;

import java.util.List;

public record OperatorTaskSummaryResponse(
    Long taskId,
    Long orderId,
    String orderLogicId,
    OrderStatus orderStatus,
    String taskType,
    String destinationLocationBarcode,
    Integer totalProcesses,
    Integer completedProcesses,
    boolean readyForCompletion,
    OperatorProcessSummaryResponse currentProcess,
    List<OperatorOrderLineSummaryResponse> orderLines,
    List<OperatorProcessSummaryResponse> processes
) {
}
