package com.isd.wms.service.process;

import com.isd.wms.entity.Process;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.TaskType;

public interface ProcessCompletionStrategy {

    void handle(Process process);

    void updateStatus(Task task);

    boolean support(TaskType taskType);
}
