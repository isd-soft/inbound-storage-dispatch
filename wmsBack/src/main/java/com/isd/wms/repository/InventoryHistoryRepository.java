package com.isd.wms.repository;

import com.isd.wms.entity.InventoryHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    List<InventoryHistory> findByProductIdAndSkuIgnoreCaseAndSourceLocationIdOrProductIdAndSkuIgnoreCaseAndDestinationLocationId(
            Long sourceProductId,
            String sourceSku,
            Long sourceLocationId,
            Long destinationProductId,
            String destinationSku,
            Long destinationLocationId
    );
}
