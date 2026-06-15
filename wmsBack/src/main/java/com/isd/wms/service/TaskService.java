package com.isd.wms.service;

import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.TaskType;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.TaskNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProcessRepository processRepository;
    private final ReplenishmentRepository replenishmentRepository;
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

    @Transactional
    public Task assignTask(Long taskId, Long operatorId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
        User operator = userRepository.findById(operatorId)
            .orElseThrow(() -> new UserNotFoundException(operatorId));

        if (task.getOperator().filter(current -> current.equals(operator)).isPresent()) {
            return task;
        }

        task.setOperator(operator);
        taskRepository.save(task);

        var processes = processRepository.findAllByTaskId(taskId);
        processes.forEach((process) -> process.setStatus(Status.ASSIGNED));
        processRepository.saveAll(processes);
        
        replenishmentRepository.findByTaskId(taskId).ifPresent(replenishment -> {
            replenishment.setStatus(Status.ASSIGNED);
            replenishmentRepository.save(replenishment);
        });

        return task;
    }
}