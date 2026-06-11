package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.ReplenishmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.logicId = :logicId)
            AND (:destinationId IS NULL OR o.destinationLocation.id = :destinationId)
            AND (:status IS NULL OR o.status = :status)
            AND (:createdAt IS NULL OR o.createdAt = :createdAt)
            AND (:updatedAt IS NULL OR o.updatedAt = :updatedAt)
            """)
    List<Order> filter(
            @Param("logicId") String logicId,
            @Param("destinationId") Long destinationId,
            @Param("status") OrderStatus status,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

}
