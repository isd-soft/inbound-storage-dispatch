package com.isd.wms.mapper;

import com.isd.wms.dto.order.ExtendedOrderResponse;
import com.isd.wms.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ExtendedOrderMapper {
    private final OrderMapper orderMapper;
    private final OrderLineMapper orderLineMapper;

    public ExtendedOrderResponse toResponse(Order order) {
        return new ExtendedOrderResponse(
                orderMapper.toResponse(order),
                order.getOrderLines().stream().map(orderLineMapper::toResponse).toList()
        );
    }
}