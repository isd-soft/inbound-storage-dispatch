package com.isd.wms.repository;

import com.isd.wms.entity.ReplenishmentTask;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ReplenishmentTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplenishmentTaskRepository extends JpaRepository<ReplenishmentTask, Long> {

    List<ReplenishmentTask> findReplenishmentTasksByOperator(User operator);

    List<ReplenishmentTask> findReplenishmentTasksByStatus(ReplenishmentTaskStatus status);

    @Query("""
            SELECT r FROM ReplenishmentTask r
            WHERE (:productId IS NULL OR r.product.id = :productId)
            AND (:operatorId IS NULL OR r.operator.id = :operatorId)
            AND (:requestedQuantity IS NULL OR r.requestedQuantity = :requestedQuantity)
            AND (:status IS NULL OR r.status = :status)
            AND (:sourceLocationId IS NULL OR r.sourceLocation.id = :sourceLocationId)
            AND (:destinationLocationId IS NULL OR r.destinationLocation.id = :destinationLocationId)
            """)
    List<ReplenishmentTask> filter(
            @Param("productId") Long productId,
            @Param("operatorId") Long operatorId,
            @Param("requestedQuantity") Integer requestedQuantity,
            @Param("status") ReplenishmentTaskStatus status,
            @Param("sourceLocationId") Long sourceLocationId,
            @Param("destinationLocationId") Long destinationLocationId
    );
}
