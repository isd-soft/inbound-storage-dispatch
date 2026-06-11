package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.sql.Timestamp;
import java.util.List;

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
            @Param("status") Status status,
            @Param("createdAt") Timestamp createdAt,
            @Param("updatedAt") Timestamp updatedAt
    );

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN OrderLine ol ON ol.order = o
        JOIN Process p ON p.task = ol.task
        WHERE p.operator = :operator
        ORDER BY o.createdAt
    """)
    List<Order> findOldestOrderByOperator(
        @Param("operator") User operator,
        Pageable pageable
    );
}
