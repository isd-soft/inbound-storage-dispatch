package com.isd.wms.service.process;

import com.isd.wms.entity.Process;
import com.isd.wms.enums.TaskType;

public interface ProcessCompletionStrategy {

    void handle(Process process);

    boolean support(TaskType taskType);
}
