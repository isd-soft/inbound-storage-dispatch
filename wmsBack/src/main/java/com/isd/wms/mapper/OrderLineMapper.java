package com.isd.wms.mapper;

import com.isd.wms.dto.order_line.OrderLineResponse;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

@Component
public class OrderLineMapper {
    public OrderLineResponse toResponse(OrderLine orderLine
    ) {
        Task task = orderLine.getTask().orElseThrow(() -> new InvalidRequestException("No task found for order line " + orderLine.getId()));
        return new OrderLineResponse(
                orderLine.getId(),
                orderLine.getOrder().getId(),
                task.getId(),
                orderLine.getProduct().getId(),
                orderLine.getRequestedQuantity(),
                orderLine.getStatus(),
                orderLine.getCreatedAt(),
                orderLine.getUpdatedAt()
        );
    }
}
