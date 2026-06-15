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

@Component
@RequiredArgsConstructor
public class PickingAllocationCompletionStrategy implements AllocationCompletionStrategy {

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;

    @Override
    public boolean updateStatus(Task task) {
        OrderLine orderLine = orderLineRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("No order found for task!"));

        orderLine.setStatus(Status.COMPLETED);
        orderLineRepository.save(orderLine);

        return orderRepository.markOrderAsCompleted(orderLine.getOrder(), OrderStatus.COMPLETED) > 0;
    }

    @Override
    public AllocationCompletionResult result(Task task) {
        Order order = orderRepository.getOrderByTask(task)
            .orElseThrow(() -> new RuntimeException("No order found for task with id " + task.getId()));
        return new AllocationCompletionResult(
            order.getStatus() == OrderStatus.COMPLETED ? AllocationCompletionStatus.COMPLETED : AllocationCompletionStatus.PICKING,
            TaskType.PICKING_ORDER,
            order.getId()
        );
    }

    @Override
    public boolean support(TaskType taskType) {
        return taskType == TaskType.PICKING_ORDER;
    }
}
