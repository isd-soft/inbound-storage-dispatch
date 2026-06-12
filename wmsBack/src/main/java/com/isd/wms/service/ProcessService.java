package com.isd.wms.service;

import com.isd.wms.repository.projections.OperatorProcessProjection;
import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.dto.process.ShortProcessResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProcessesNotFoundException;
import com.isd.wms.mapper.ProcessMapper;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.service.validation.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (process.getTask().getOperator().filter(operator::equals).isEmpty()) {
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

    public ProcessOperatorResponse getProcessesOperator() {
        String username = securityFacade.getCurrentUsername();
        OperatorProcessProjection info = getProcessForOperator(username);
        Long oldestOrder = info.getOldestOrderId();
        Integer total = processRepository.countProcessesInOrder(oldestOrder);
        Integer current = processRepository.countCompletedProcessesInOrder(oldestOrder) + 1;

        return new ProcessOperatorResponse(
            total, current, info.getOrderName(),
            new ShortProcessResponse(
                info.getProcessId(),
                info.getProductName(),
                info.getProductBarcode(),
                info.getLocationName(),
                info.getLocationBarcode(),
                info.getQuantity()
            )
        );
    }

    private @NonNull OperatorProcessProjection getProcessForOperator(String username) {
        return processRepository.getProcessInfoForOperator(username)
            .orElseThrow(() -> new ProcessesNotFoundException(username));
    }
}
