package com.isd.wms.service.allocation;

import com.isd.wms.dto.allocation.AllocationCompletionResult;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.AllocationCompletionStatus;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PickingAllocationCompletionStrategy implements AllocationCompletionStrategy {

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;

    @Override
    public boolean updateStatus(Task task) {
        OrderLine orderLine = orderLineRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("No order found for task!"));

        orderLine.setStatus(resolveLineStatus(orderLine));
        orderLineRepository.save(orderLine);

        Order order = orderLine.getOrder();
        List<OrderLine> orderLines = orderLineRepository.findAllByOrderId(order.getId());
        if (orderLines.stream().anyMatch(line -> line.getStatus() != Status.COMPLETED
            && line.getStatus() != Status.CANCELED
            && line.getStatus() != Status.PARTIALLY_COMPLETED)) {
            return false;
        }

        OrderStatus finalStatus = computeFinalStatus(orderLines);
        order.setStatus(finalStatus);
        orderRepository.save(order);
        return true;
    }

    @Override
    public AllocationCompletionResult result(Task task) {
        Order order = orderRepository.getOrderByTask(task)
            .orElseThrow(() -> new RuntimeException("No order found for task with id " + task.getId()));
        return new AllocationCompletionResult(
            order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.PARTIALLY_COMPLETED || order.getStatus() == OrderStatus.CANCELED
                ? AllocationCompletionStatus.COMPLETED
                : AllocationCompletionStatus.PICKING,
            TaskType.PICKING_ORDER,
            order.getId()
        );
    }

    @Override
    public boolean support(TaskType taskType) {
        return taskType == TaskType.PICKING_ORDER;
    }

    private OrderStatus computeFinalStatus(List<OrderLine> orderLines) {
        boolean allCanceled = !orderLines.isEmpty() && orderLines.stream().allMatch(line -> line.getStatus() == Status.CANCELED);
        if (allCanceled) {
            return OrderStatus.CANCELED;
        }

        boolean hasPartialHistory = orderLines.stream().anyMatch(line ->
            line.getStatus() == Status.CANCELED
                || line.getStatus() == Status.SHORTAGE
                || resolveDeliveredQuantity(line) < line.getRequestedQuantity()
                || line.getShortageQuantity() > 0
                || line.getStatus() == Status.PARTIALLY_COMPLETED
        );
        return hasPartialHistory ? OrderStatus.PARTIALLY_COMPLETED : OrderStatus.COMPLETED;
    }

    private int resolveDeliveredQuantity(OrderLine line) {
        Integer deliveredQuantity = line.getDeliveredQuantity();
        if (deliveredQuantity != null && deliveredQuantity > 0) {
            return deliveredQuantity;
        }

        return line.getTask().orElseThrow().getAllocations().stream()
            .filter(allocation -> allocation.getStatus() != Status.CANCELED)
            .mapToInt(allocation -> Optional.ofNullable(allocation.getQuantity()).orElse(0))
            .sum();
    }

    private Status resolveLineStatus(OrderLine orderLine) {
        int deliveredQuantity = resolveDeliveredQuantity(orderLine);
        int requestedQuantity = Optional.ofNullable(orderLine.getRequestedQuantity()).orElse(0);
        if (deliveredQuantity <= 0) {
            return Status.CANCELED;
        }
        if (deliveredQuantity < requestedQuantity) {
            boolean hasPendingAllocations = orderLine.getTask() != null
                && orderLine.getTask().getAllocations().stream().anyMatch(allocation ->
                    allocation.getStatus() == Status.ASSIGNED
                        || allocation.getStatus() == Status.IN_PROGRESS
                        || allocation.getStatus() == Status.CREATED
                );
            if (hasPendingAllocations) {
                return Status.SHORTAGE;
            }
            return Status.PARTIALLY_COMPLETED;
        }
        return Status.COMPLETED;
    }
}
