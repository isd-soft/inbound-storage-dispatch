package com.isd.wms.service;

import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final SecurityFacade securityFacade;
    private final WorkflowService workflowService;

    public Task createTask(TaskType type, Integer requestedQuantity, Long productId) {
        User supervisor = securityFacade.getCurrentUser();

        log.info("Task creation initiated by Supervisor '{}'. Type: {}, Requested Qty: {}, Target Product ID: {}",
            supervisor.getUsername(), type, requestedQuantity, productId);

        Task task = new Task(supervisor, type, requestedQuantity);
        task = taskRepository.save(task);

        log.info("Parent Task successfully persisted. System ID: {}, Status: {}", task.getId(), task.getStatus());
        log.debug("Handing over control to WorkflowService to resolve stock and generate sub-processes for Task ID: {}", task.getId());

        workflowService.generateProcessesForTask(task, productId, requestedQuantity);
        log.info("Workflow execution completed successfully for Task ID: {}. Sub-processes generated.", task.getId());

        return task;
    }

}
