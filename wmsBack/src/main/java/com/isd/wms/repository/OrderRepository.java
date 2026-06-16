package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT o FROM Order o
        JOIN OrderLine ol ON ol.order = o
        JOIN Task t ON t = ol.task
        JOIN User u ON u = t.supervisor
        WHERE (:logicId IS NULL OR o.logicId = :logicId)
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

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN OrderLine ol ON ol.order = o
        JOIN Task t ON t = ol.task
        JOIN User u ON u = t.supervisor
        WHERE u.username = :username
        """)
    List<Order> findAllByCreatedByUsername(@Param("username") String username);

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN OrderLine ol ON ol.order = o
        JOIN Task t ON t = ol.task
        JOIN User u ON u = t.supervisor
        WHERE o.id = :id AND u.username = :username
        """)
    Optional<Order> findByIdAndCreatedByUsername(@Param("id") Long id, @Param("username") String username);

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

    @Query(value = """
        SELECT DISTINCT o.* FROM orders o
        JOIN order_lines ol ON o.id = ol.order_id
        JOIN tasks t ON ol.task_id = t.id
        WHERE t.operator_id = :operatorId
          AND o.status = 'PICKED'
        ORDER BY o.created_at, o.id
        LIMIT 1
    """, nativeQuery = true)
    Optional<Order> findOldestPickedOrderAssignedToOperator(
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

    @Modifying
    @Query("""
            UPDATE Order o
                SET o.status = :orderStatus
                WHERE o = :order
                    AND NOT EXISTS (
                          SELECT 1 FROM OrderLine ol
                          WHERE ol.order = o
                            AND ol.status NOT IN (COMPLETED, CANCELED)
                      )
        """)
    int markOrderAsCompleted(
        @Param("order") Order order,
        @Param("orderStatus") OrderStatus orderStatus
    );

    @Query("""
        SELECT o FROM Order o
        JOIN OrderLine ol ON ol.order = o
        WHERE ol.task = :task
    """)
    Optional<Order> getOrderByTask(
        @Param("task") Task task
    );
}
