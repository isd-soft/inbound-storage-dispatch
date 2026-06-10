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
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}