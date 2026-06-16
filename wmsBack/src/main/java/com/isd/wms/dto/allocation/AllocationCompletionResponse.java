package com.isd.wms.dto.allocation;

import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.TaskType;

public record AllocationCompletionResponse(
    AllocationCompletionStatus status,
    TaskType taskType,
    Long id
) {
    public AllocationCompletionResponse(AllocationCompletionResult result) {
        this(result.status(), result.taskType(), result.id());
    }
}
