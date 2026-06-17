package com.isd.wms.mapper;

import com.isd.wms.dto.order.OrderResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order
    ) {
        Long operatorId = getOperatorId(order);
        return new OrderResponse(
            order.getId(),
            order.getLogicId(),
            order.getDestinationLocation().getId(),
            order.getStatus(),
            operatorId,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private static @Nullable Long getOperatorId(Order order) {
        return order.getOrderLines().stream()
            .map(OrderLine::getTask)
            .flatMap(Optional::stream)
            .map(Task::getOperator)
            .flatMap(Optional::stream)
            .map(User::getId)
            .findFirst()
            .orElse(null);
    }
}
