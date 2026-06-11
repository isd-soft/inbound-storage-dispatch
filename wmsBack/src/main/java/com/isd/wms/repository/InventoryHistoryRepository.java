package com.isd.wms.repository;

import com.isd.wms.entity.InventoryHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    List<InventoryHistory> findByProductIdAndSourceLocationIdOrProductIdAndDestinationLocationId(
            Long sourceProductId,
            Long sourceLocationId,
            Long destinationProductId,
            Long destinationLocationId
    );
}
