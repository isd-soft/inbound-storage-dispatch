package com.isd.wms.dto.process;

import com.isd.wms.enums.ProcessCompletionStatus;
import com.isd.wms.enums.TaskType;

public record ProcessCompletionResult(
    ProcessCompletionStatus status,
    TaskType taskType,
    Long id
) {
}
