package com.isd.wms.service.process;

import com.isd.wms.dto.process.ProcessCompletionResult;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.ProcessCompletionStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.exception.OrderNotFoundException;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PickingProcessCompletionStrategy implements ProcessCompletionStrategy {

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;

    @Override
    public boolean updateStatus(Task task) {
        OrderLine orderLine = orderLineRepository.findByTaskId(task.getId())
            .orElseThrow(() -> new RuntimeException("No order found for task!"));

        orderLine.setStatus(Status.COMPLETED);
        orderLineRepository.save(orderLine);

        return orderRepository.markOrderAsCompleted(orderLine.getOrder()) > 0;
    }

    @Override
    public ProcessCompletionResult result(Task task) {
        Order order = orderRepository.getOrderByTask(task)
            .orElseThrow(() -> new RuntimeException("No order found for task with id " + task.getId()));
        return new ProcessCompletionResult(
            order.getStatus() == OrderStatus.COMPLETED? ProcessCompletionStatus.COMPLETED : ProcessCompletionStatus.PICKING,
            TaskType.PICKING_ORDER,
            order.getId()
        );
    }

    @Override
    public boolean support(TaskType taskType) {
        return taskType == TaskType.PICKING_ORDER;
    }
}
