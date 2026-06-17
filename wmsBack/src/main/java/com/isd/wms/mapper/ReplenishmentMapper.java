package com.isd.wms.mapper;

import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ReplenishmentMapper {
    public ReplenishmentResponse toResponse(Replenishment replenishment) {
        Product product = replenishment.getProduct();
        Location destinationLocation = replenishment.getDestinationLocation();
        Long operatorId = replenishment.getTask()
            .flatMap(Task::getOperator)
            .map(User::getId)
            .orElse(null);

        return new ReplenishmentResponse(
            replenishment.getId(),
            replenishment.getTask()
                .map(Task::getId)
                .orElse(null),
            product.getId(),
            replenishment.getRequestedQuantity(),
            replenishment.getStatus(),
            destinationLocation.getId(),
            operatorId,
            replenishment.getCreatedAt()
        );
    }
}
