package com.isd.wms.mapper;

import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class ReplenishmentMapper {
    public ReplenishmentResponse toResponse(Replenishment replenishment) {
        Product product = replenishment.getProduct();
        Task task = getTask(replenishment);
        Location destinationLocation = replenishment.getDestinationLocation();

        return new ReplenishmentResponse(
            replenishment.getId(),
            task.getId(),
            product.getId(),
            replenishment.getRequestedQuantity(),
            replenishment.getStatus(),
            destinationLocation.getId(),
            replenishment.getCreatedAt()
        );
    }

    private Task getTask(Replenishment replenishment) {
        return replenishment.getTask()
            .orElse(null);
    }
}
