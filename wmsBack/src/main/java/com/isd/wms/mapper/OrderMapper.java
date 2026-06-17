package com.isd.wms.mapper;

import com.isd.wms.dto.order.OrderResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order
    ) {
        return new OrderResponse(
            order.getId(),
            order.getLogicId(),
            order.getDestinationLocation().getId(),
            order.getStatus(),
            order.getOrderLines().stream()
                .map(orderLine -> getTask(orderLine).getOperator().map(User::getId).orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private static Task getTask(OrderLine orderLine) {
        return orderLine.getTask()
            .orElseThrow(() -> new InvalidRequestException("No task found for order line " + orderLine.getId()));
    }
}
