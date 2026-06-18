package com.isd.wms.mapper;

import com.isd.wms.dto.order.OrderResponse;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.TransportUnit;
import com.isd.wms.entity.User;
import com.isd.wms.repository.TransportUnitRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final TransportUnitRepository transportUnitRepository;

    public OrderResponse toResponse(Order order) {
        Long operatorId = getOperatorId(order);

        String tuBarcode = transportUnitRepository.findByOrder(order)
            .map(TransportUnit::getBarcode)
            .orElse(null);

        return new OrderResponse(
            order.getId(),
            order.getLogicId(),
            order.getDestinationLocation().getId(),
            order.getStatus(),
            operatorId,
            tuBarcode,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private static @Nullable Long getOperatorId(Order order) {
        return order.getOrderLines().stream()
            .map(OrderLine::getTask)
            .filter(Objects::nonNull)
            .map(Task::getOperator)
            .flatMap(Optional::stream)
            .map(User::getId)
            .findFirst()
            .orElse(null);
    }
}
