package com.isd.wms.mapper;

import com.isd.wms.dto.order.OrderResponse;
import com.isd.wms.entity.Order;
import org.springframework.stereotype.Component;

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
                        .map(orderLine -> orderLine.getTask().getOperator().map(user -> user.getId()).orElse(null))
                        .filter(operatorId -> operatorId != null)
                        .findFirst()
                        .orElse(null),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
