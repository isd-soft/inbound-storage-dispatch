package com.isd.wms.service;

import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.mapper.ProcessMapper;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProcessService {

    private final ProcessRepository processRepository;
    private final WorkflowService workflowService;
    private final SecurityFacade securityFacade;
    private final ProcessMapper processMapper;
    private final OrderService orderService;

//    public List<ProcessOperatorResponse> getAvailableProcesses() {
//        List<Process> processes = processRepository.findByStatus(Status.CREATED);
//        return processes.stream().map(processMapper::toResponse).toList();
//    }

//    public List<ProcessOperatorResponse> getMyProcesses() {
//        User operator = securityFacade.getCurrentUser();
//
//        List<Process> processes = processRepository.findByOperatorAndStatuses(
//                operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS));
//        return processes.stream().map(processMapper::toResponse).toList();
//    }

    @Transactional
    public ProcessResponse completeProcess(Long processId) {
        Process process = getProcessById(processId);
        User operator = securityFacade.getCurrentUser();

        if (process.getOperator().filter(operator::equals).isEmpty()) {
            throw new InvalidRequestException("You can only complete your own processes");
        }

        if (process.getStatus() == Status.COMPLETED || process.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Process is already completed or canceled");
        }

        process.setStatus(Status.COMPLETED);
        process = processRepository.save(process);

        workflowService.executeProcessCompletion(process);

        return processMapper.toResponse(process);
    }

    private Process getProcessById(Long processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found with id: " + processId));
    }

    public List<ProcessOperatorResponse> getProcessesOperator() {
        User operator = securityFacade.getCurrentUser();
        Order oldestOrder = orderService.getOldestOrder(operator);
        List<Process> processes = processRepository.findOldestProcessesByOrder(oldestOrder, operator);
        Integer length = processes.size();
        return processes
            .stream()
            .map((p) -> processMapper.toOperatorResponse(p,length)).toList();
    }
}
