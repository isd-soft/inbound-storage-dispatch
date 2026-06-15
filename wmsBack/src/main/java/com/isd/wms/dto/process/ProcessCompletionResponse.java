package com.isd.wms.dto.process;

import com.isd.wms.enums.ProcessCompletionStatus;
import com.isd.wms.enums.TaskType;

public record ProcessCompletionResponse(
    ProcessCompletionStatus status,
    TaskType taskType,
    Long id
) {
    public ProcessCompletionResponse(ProcessCompletionResult result) {
        this(result.status(), result.taskType(), result.id());
    }
}
