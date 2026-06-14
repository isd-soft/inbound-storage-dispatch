package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.User;
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
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

    @Query(value = """
        SELECT o.* FROM orders o
        JOIN order_lines ol ON o.id = ol.order_id
        JOIN tasks t ON ol.task_id = t.id
        WHERE t.operator_id = :operatorId
        AND o.status LIKE 'ASSIGNED'
        LIMIT 1
    """, nativeQuery = true)
    Optional<Order> findOldestOrderAssignedToOperator(
        @Param("operatorId") Long operatorId
    );

    @Modifying
    @Query("""
        UPDATE Order o
            SET o.status = :orderStatus
            WHERE o.id = :orderId
    """)
    int updateStatus(
        @Param("orderId") Long orderId,
        @Param("orderStatus") OrderStatus orderStatus);
}
