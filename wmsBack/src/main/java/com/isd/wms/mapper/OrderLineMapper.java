package com.isd.wms.mapper;

import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.entity.OrderLine;
import org.springframework.stereotype.Component;

@Component
public class OrderLineMapper {
    public OrderLineResponse toResponse(OrderLine orderLine
    ) {
        return new OrderLineResponse(
                orderLine.getId(),
                orderLine.getOrder().getId(),
                orderLine.getTask().getId(),
                orderLine.getProduct().getId(),
                orderLine.getRequestedQuantity(),
                orderLine.getStatus(),
                orderLine.getDestinationLocation().getId(),
                orderLine.getCreatedAt(),
                orderLine.getUpdatedAt()
        );
    }
}