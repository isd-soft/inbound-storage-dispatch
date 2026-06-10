package com.isd.wms.service;

import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;

    public Task createTask(TaskType type, Integer requestedQuantity, Long productId) {
        User supervisor = getUser(getCurrentUsername());

        Task task = new Task(supervisor, type, requestedQuantity);
        task = taskRepository.save(task);

        workflowService.generateProcessesForTask(task, productId, requestedQuantity);

        return task;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
