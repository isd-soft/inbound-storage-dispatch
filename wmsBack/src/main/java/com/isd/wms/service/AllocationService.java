package com.isd.wms.service;

import com.isd.wms.dto.allocation.AllocationResponse;
import com.isd.wms.dto.allocation.AllocationOperatorResponse;
import com.isd.wms.dto.allocation.ShortAllocationResponse;
import com.isd.wms.entity.*;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.AllocationsNotFoundException;
import com.isd.wms.mapper.AllocationMapper;
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.ReplenishmentRepository;
import com.isd.wms.repository.projections.AllocationSupervisorProjection;
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
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final WorkflowService workflowService;
    private final SecurityFacade securityFacade;
    private final AllocationMapper AllocationMapper;
    private final OrderLineRepository orderLineRepository;
    private final ReplenishmentRepository replenishmentRepository;

    public List<AllocationSupervisorProjection> getAllAllocations() {
        return allocationRepository.getAllAllocationsSupervisor(securityFacade.getCurrentUsername());
    }

    @Transactional
    public AllocationResponse completeAllocation(Long allocationId) {
        Allocation allocation = getAllocationById(allocationId);
        User operator = securityFacade.getCurrentUser();

        if (allocation.getTask().getOperator().filter(operator::equals).isEmpty()) {
            throw new InvalidRequestException("You can only complete your own allocations");
        }

        if (allocation.getStatus() == Status.COMPLETED || allocation.getStatus() == Status.CANCELED) {
            throw new InvalidRequestException("Allocation is already completed or canceled");
        }

        allocation.setStatus(Status.COMPLETED);
        allocation = allocationRepository.save( allocation);

        workflowService.executeAllocationCompletion(allocation);

        return AllocationMapper.toResponse(allocation);
    }

    private Allocation getAllocationById(Long allocationId) {
        return allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found with id: " + allocationId));
    }

    public AllocationOperatorResponse getAllocationsOperator() {
        String username = securityFacade.getCurrentUsername();
        Allocation allocation = getAllocationsForOperator(username);
        Task task = allocation.getTask();
        TaskType taskType = task.getTaskType();

        Integer total = allocationRepository.findAllByTaskId(task.getId()).size();
        Integer current = Math.toIntExact(allocationRepository.findAllByTaskId(task.getId()).stream()
            .filter(taskAllocation -> taskAllocation.getStatus() == Status.COMPLETED)
            .count()) + 1;
        String logicalId = null;
        String destinationLocationBarcode = null;

        if (taskType == TaskType.PICKING_ORDER) {
            OrderLine orderLine = orderLineRepository.findByTaskId(task.getId())
                .orElseThrow(() -> new AllocationsNotFoundException(username));
            Order order = orderLine.getOrder();
            logicalId = order.getLogicId();
            destinationLocationBarcode = order.getDestinationLocation().getBarcode();
            total = allocationRepository.countAllocationsInOrder(order.getId());
            current = allocationRepository.countCompletedAllocationsInOrder(order.getId()) + 1;
        } else if (taskType == TaskType.REPLENISHMENT) {
            Replenishment replenishment = replenishmentRepository.findByTaskId(task.getId())
                .orElseThrow(() -> new AllocationsNotFoundException(username));
            destinationLocationBarcode = replenishment.getDestinationLocation().getBarcode();
        }

        return new AllocationOperatorResponse(
            total, current, logicalId, taskType.name(), destinationLocationBarcode,
            new ShortAllocationResponse(
                allocation.getId(),
                allocation.getStock().getProduct().map(product -> product.getName()).orElse(null),
                allocation.getStock().getProduct().map(product -> product.getBarcode()).orElse(null),
                allocation.getStock().getLocation().getName(),
                allocation.getStock().getLocation().getBarcode(),
                allocation.getQuantity()
            )
        );
    }

    private Allocation getAllocationsForOperator(String username) {
        return allocationRepository.findFirstByTaskAndOperatorAndUsernameAndStatusInOrderByCreatedAtAscIdAsc(
                username,
                java.util.List.of(Status.ASSIGNED, Status.IN_PROGRESS)
            )
            .orElseThrow(() -> new AllocationsNotFoundException(username));
    }
}
