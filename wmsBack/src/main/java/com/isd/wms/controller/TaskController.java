package com.isd.wms.controller;

import com.isd.wms.entity.Task;
import com.isd.wms.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping("/{taskId}/operators/{operatorId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<Task> assignTask(@PathVariable Long taskId, @PathVariable Long operatorId) {
        return ResponseEntity.ok(taskService.assignTask(taskId, operatorId));
    }
}
