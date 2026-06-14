package com.isd.wms.service.process;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import com.isd.wms.enums.TaskType;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PickingProcessCompletionStrategy implements ProcessCompletionStrategy {

    private final ProcessRepository processRepository;
    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;

    @Override
    public void handle(Process process) {
        Task task = process.getTask();

        List<Process> allProcesses = processRepository.findAllByTaskId(task.getId());
        boolean isTaskFullyCompleted = allProcesses.stream()
            .allMatch(p -> p.getStatus() == Status.COMPLETED || p.getId().equals(process.getId()));

        if (isTaskFullyCompleted) {
            orderLineRepository.findByTaskId(task.getId()).ifPresent(orderLine -> {
                orderLine.setStatus(Status.COMPLETED);
                orderLineRepository.save(orderLine);

                Order order = orderLine.getOrder();
                boolean orderCompleted = order.getOrderLines().stream()
                    .allMatch(line -> line.getStatus() == Status.COMPLETED);

                if (orderCompleted) {
                    order.setStatus(OrderStatus.COMPLETED);
                    orderRepository.save(order);
                }
            });
        }
    }

    @Override
    public boolean support(TaskType taskType) {
        return taskType == TaskType.PICKING_ORDER;
    }
}
