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
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.UserRepository;
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
        log.info("Request to complete Process ID: {} initiated by operator '{}'", processId, operator.getUsername());

        if (process.getTask().getOperator().filter(operator::equals).isEmpty()) {
            log.warn("Security/Workflow violation: Operator '{}' attempted to complete Process ID {} which is not assigned to them.",
                operator.getUsername(), processId);
            throw new InvalidRequestException("You can only complete your own processes");
        }

        if (process.getStatus() == Status.COMPLETED || process.getStatus() == Status.CANCELED) {
            log.warn("Process completion rejected: Process ID {} is already in a terminal state ({})", processId, process.getStatus());
            throw new InvalidRequestException("Process is already completed or canceled");
        }

        Status oldStatus = process.getStatus();
        process.setStatus(Status.COMPLETED);
        Process savedProcess = processRepository.save(process);

        log.info("Process ID {} state updated: {} -> COMPLETED. Triggering down-stream workflow updates.", processId, oldStatus);

        workflowService.executeProcessCompletion(savedProcess);

        log.info("Process ID {} successfully closed and finalized by operator '{}'", processId, operator.getUsername());
        return processMapper.toResponse(savedProcess);
    }

    private Process getProcessById(Long processId) {
        return processRepository.findById(processId)
            .orElseThrow(() -> {
                log.warn("Process lookup failed: Process record with ID {} does not exist", processId);
                return new InvalidRequestException("Process not found with id: " + processId);
            });
    }

    public ProcessOperatorResponse getProcessesOperator() {
        String username = securityFacade.getCurrentUsername();
        log.debug("Fetching current active process dashboard data for operator user: '{}'", username);

        OperatorProcessProjection info = getProcessForOperator(username);
        Long oldestOrder = info.getOldestOrderId();
        Integer total = processRepository.countProcessesInOrder(oldestOrder);
        Integer current = processRepository.countCompletedProcessesInOrder(oldestOrder) + 1;

        log.debug("Dashboard payload calculated for operator '{}'. Order ID: {}, Progress: {}/{}",
            username, oldestOrder, current, total);

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
            .orElseThrow(() -> {
                log.warn("Dashboard lookup empty: No active assigned processes found for operator '{}'", username);
                return new ProcessesNotFoundException(username);
            });
    }
}
