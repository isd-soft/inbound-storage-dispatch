package com.isd.wms.service.process;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.TaskType;

public interface ProcessCompletionStrategy {

    default void handle(Allocation allocation) {}

    boolean updateStatus(Task task);

    boolean support(TaskType taskType);

    AllocationCompletionResult result(Task task);
}
