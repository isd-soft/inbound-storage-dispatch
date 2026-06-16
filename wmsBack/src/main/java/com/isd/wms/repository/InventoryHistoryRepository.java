package com.isd.wms.repository;

import com.isd.wms.entity.InventoryHistory;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    List<InventoryHistory> findByProductIdAndSourceLocationIdOrProductIdAndDestinationLocationId(
            Long sourceProductId,
            Long sourceLocationId,
            Long destinationProductId,
            Long destinationLocationId
    );

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM InventoryHistory h
        WHERE h.product.id = :productId
          AND (
            (h.sourceLocation IS NOT NULL AND h.sourceLocation.id = :locationId)
            OR (h.destinationLocation IS NOT NULL AND h.destinationLocation.id = :locationId)
          )
    """)
    int deleteRelatedToStock(
        @Param("productId") Long productId,
        @Param("locationId") Long locationId
    );
}
