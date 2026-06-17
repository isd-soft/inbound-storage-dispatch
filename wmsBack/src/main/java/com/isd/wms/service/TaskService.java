package com.isd.wms.service;

import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.TaskType;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.TaskNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import com.isd.wms.repository.AllocationRepository  ;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AllocationRepository  allocationRepository ;
    private final ReplenishmentRepository replenishmentRepository;
    private final SecurityFacade securityFacade;
    private final WorkflowService workflowService;

    public Task createTask(TaskType type, Integer requestedQuantity, Long productId) {
        User supervisor = securityFacade.getCurrentUser();

        Task task = new Task(supervisor, type, requestedQuantity);
        task = taskRepository.save(task);

        workflowService.generateAllocationsForTask(task, productId, requestedQuantity);

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

        var allocations = allocationRepository.findAllByTaskId(taskId);
        allocations.forEach((allocation) -> allocation.setStatus(Status.ASSIGNED));

        allocationRepository.saveAll(allocations);

        replenishmentRepository.findByTaskId(taskId).ifPresent(replenishment -> {
            replenishment.setStatus(Status.ASSIGNED);
            replenishmentRepository.save(replenishment);
        });

        return task;
    }

}
