package com.isd.wms.service;

import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ProcessStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessService {

    private final ProcessRepository processRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;

    public List<ProcessResponse> getAvailableProcesses() {
        List<Process> processes = processRepository.findByStatus(ProcessStatus.CREATED);
        return processes.stream().map(this::toResponse).toList();
    }

    public List<ProcessResponse> getMyProcesses() {
        User operator = getCurrentUser();
        List<Process> processes = processRepository.findByOperatorAndStatuses(
                operator, List.of(ProcessStatus.ASSIGNED, ProcessStatus.IN_PROGRESS));
        return processes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProcessResponse assignProcess(Long processId) {
        Process process = getProcessById(processId);
        User operator = getCurrentUser();

        if (process.getStatus() != ProcessStatus.CREATED) {
            throw new InvalidRequestException("Process is already assigned or completed");
        }

        process.setOperator(operator);
        process.setStatus(ProcessStatus.ASSIGNED);

        return toResponse(processRepository.save(process));
    }

    @Transactional
    public ProcessResponse completeProcess(Long processId) {
        Process process = getProcessById(processId);
        User operator = getCurrentUser();

        if (process.getOperator().filter(e -> e.equals(operator)).isEmpty()) {
            throw new InvalidRequestException("You can only complete your own processes");
        }

        if (process.getStatus() == ProcessStatus.COMPLETED || process.getStatus() == ProcessStatus.CANCELED) {
            throw new InvalidRequestException("Process is already completed or canceled");
        }

        process.setStatus(ProcessStatus.COMPLETED);
        process = processRepository.save(process);

        workflowService.executeProcessCompletion(process);

        return toResponse(process);
    }

    private Process getProcessById(Long processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found with id: " + processId)); // Замени на ProcessNotFoundException если есть
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private ProcessResponse toResponse(Process process) {
        Stock stock = process.getStock();
        return new ProcessResponse(
                process.getId(),
                process.getTask().getId(),
                stock.getProduct().getId(),
                stock.getProduct().getName(),
                stock.getLocation().getLocationCode(),
                process.getQuantity(),
                process.getStatus()
        );
    }
}
