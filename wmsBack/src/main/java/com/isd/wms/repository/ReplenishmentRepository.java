package com.isd.wms.repository;

import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ReplenishmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplenishmentRepository extends JpaRepository<Replenishment, Long> {

    List<Replenishment> findReplenishmentTasksByOperator(User operator);

    List<Replenishment> findReplenishmentTasksByStatus(ReplenishmentStatus status);

    @Query("""
            SELECT r FROM Replenishment r
            WHERE (:taskId IS NULL OR r.task.id = :taskId)
            AND (:productId IS NULL OR r.product.id = :productId)
            AND (:requestedQuantity IS NULL OR r.requestedQuantity = :requestedQuantity)
            AND (:status IS NULL OR r.status = :status)
            AND (:destinationLocationId IS NULL OR r.destinationLocation.id = :destinationLocationId)
            """)
    List<Replenishment> filter(
            @Param("taskId") Long taskId,
            @Param("productId") Long productId,
            @Param("requestedQuantity") Integer requestedQuantity,
            @Param("status") ReplenishmentStatus status,
            @Param("destinationLocationId") Long destinationLocationId
    );
}
