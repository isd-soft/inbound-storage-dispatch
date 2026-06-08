package com.isd.wms.mapper;

import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReplenishmentMapper {
    public ReplenishmentResponse toResponse(Replenishment replenishment) {
        Product product = replenishment.getProduct();
        User operator = replenishment.getOperator();
        Location sourceLocation = replenishment.getSourceLocation();
        Location destinationLocation = replenishment.getDestinationLocation();

        return new ReplenishmentResponse(
                replenishment.getId(),
                product.getId(),
                operator.getId(),
                replenishment.getRequestedQuantity(),
                replenishment.getStatus(),
                sourceLocation.getId(),
                destinationLocation.getId(),
                replenishment.getCreatedAt()
        );
    }
}