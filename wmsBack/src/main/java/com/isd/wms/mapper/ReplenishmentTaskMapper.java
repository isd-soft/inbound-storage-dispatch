package com.isd.wms.mapper;

import com.isd.wms.dto.replenishment.ReplenishmentTaskResponse;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.ReplenishmentTask;
import com.isd.wms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReplenishmentTaskMapper {
    public ReplenishmentTaskResponse toResponse(ReplenishmentTask replenishmentTask) {
        Product product = replenishmentTask.getProduct();
        User operator = replenishmentTask.getOperator();
        Location sourceLocation = replenishmentTask.getSourceLocation();
        Location destinationLocation = replenishmentTask.getDestinationLocation();

        return new ReplenishmentTaskResponse(
                replenishmentTask.getId(),
                product.getId(),
                operator.getId(),
                replenishmentTask.getRequestedQuantity(),
                replenishmentTask.getStatus(),
                sourceLocation.getId(),
                destinationLocation.getId(),
                replenishmentTask.getCreatedAt()
        );
    }
}