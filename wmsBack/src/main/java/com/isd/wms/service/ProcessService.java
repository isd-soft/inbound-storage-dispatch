package com.isd.wms.service;

import com.isd.wms.dto.process.ProcessOperatorResponse;
import com.isd.wms.dto.process.ProcessResponse;
import com.isd.wms.dto.process.ShortProcessResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProcessesNotFoundException;
import com.isd.wms.mapper.ProcessMapper;
import com.isd.wms.repository.OrderLineRepository;
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
@Transactional(readOnly = true)
public class ProcessService {

    private final ProcessRepository processRepository;
    private final WorkflowService workflowService;
    private final SecurityFacade securityFacade;
    private final ProcessMapper processMapper;
    private final OrderLineRepository orderLineRepository;
    private final ReplenishmentRepository replenishmentRepository;

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

        Process process = getProcessForOperator(username);
        Task task = process.getTask();
        TaskType taskType = task.getTaskType();

        Integer total = processRepository.findAllByTaskId(task.getId()).size();
        Integer current = Math.toIntExact(processRepository.findAllByTaskId(task.getId()).stream()
            .filter(taskProcess -> taskProcess.getStatus() == Status.COMPLETED)
            .count()) + 1;
        
        String logicalId = null;
        String destinationLocationBarcode = null;

        if (taskType == TaskType.PICKING_ORDER) {
            OrderLine orderLine = orderLineRepository.findByTaskId(task.getId())
                .orElseThrow(() -> {
                    log.warn("Dashboard lookup empty: No active assigned processes found for operator '{}'", username);
                    return new ProcessesNotFoundException(username);
                });
            Order order = orderLine.getOrder();
            logicalId = order.getLogicId();
            destinationLocationBarcode = order.getDestinationLocation().getBarcode();
            total = processRepository.countProcessesInOrder(order.getId());
            current = processRepository.countCompletedProcessesInOrder(order.getId()) + 1;
        } else if (taskType == TaskType.REPLENISHMENT) {
            Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
                .orElseThrow(() -> {
                    log.warn("Dashboard lookup empty: No active replenishment tasks found for operator '{}'", username);
                    return new ProcessesNotFoundException(username);
                });
            destinationLocationBarcode = replenishment.getDestinationLocation().getBarcode();
        }

        log.debug("Dashboard payload calculated for operator '{}'. Task Type: {}, Progress: {}/{}",
            username, taskType, current, total);

        return new ProcessOperatorResponse(
            total, current, logicalId, taskType.name(), destinationLocationBarcode,
            new ShortProcessResponse(
                process.getId(),
                process.getStock().getProduct().map(product -> product.getName()).orElse(null),
                process.getStock().getProduct().map(product -> product.getBarcode()).orElse(null),
                process.getStock().getLocation().getName(),
                process.getStock().getLocation().getBarcode(),
                process.getQuantity()
            )
        );
    }

    private Process getProcessForOperator(String username) {
        return processRepository.findFirstByTask_Operator_UsernameAndStatusInOrderByCreatedAtAscIdAsc(
                username,
                java.util.List.of(Status.ASSIGNED, Status.IN_PROGRESS)
            )
            .orElseThrow(() -> {
                log.warn("Dashboard lookup empty: No active assigned processes found for operator '{}'", username);
                return new ProcessesNotFoundException(username);
            });
    }
}