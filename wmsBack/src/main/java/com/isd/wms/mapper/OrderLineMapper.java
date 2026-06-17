package com.isd.wms.mapper;

import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.Status;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderLineMapper {
    public OrderLineResponse toResponse(OrderLine orderLine
    ) {
        Long taskId = orderLine.getTask() == null ? null : orderLine.getTask().getId();
        return new OrderLineResponse(
                orderLine.getId(),
                orderLine.getOrder().getId(),
                taskId,
                orderLine.getProduct().getId(),
                orderLine.getRequestedQuantity(),
                resolveDeliveredQuantity(orderLine),
                orderLine.getStatus(),
                orderLine.getCreatedAt(),
                orderLine.getUpdatedAt()
        );
    }

    private Integer resolveDeliveredQuantity(OrderLine orderLine) {
        Integer deliveredQuantity = orderLine.getDeliveredQuantity();
        if (deliveredQuantity != null && deliveredQuantity > 0) {
            return deliveredQuantity;
        }

        Task task = orderLine.getTask();
        if (task == null) {
            return 0;
        }

        return task.getAllocations().stream()
            .filter(allocation -> allocation.getStatus() != Status.CANCELED)
            .mapToInt(allocation -> Optional.ofNullable(allocation.getQuantity()).orElse(0))
            .sum();
    }
}
