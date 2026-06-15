package com.isd.wms.service.process;

import com.isd.wms.dto.process.ProcessCompletionResponse;
import com.isd.wms.dto.process.ProcessCompletionResult;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.TaskType;

public interface ProcessCompletionStrategy {

    default void handle(Process process) {}

    boolean updateStatus(Task task);

    boolean support(TaskType taskType);

    ProcessCompletionResult result(Task task);
}
