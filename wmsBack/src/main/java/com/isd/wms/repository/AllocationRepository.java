package com.isd.wms.repository;

import com.isd.wms.repository.projections.AllocationSupervisorProjection;
import com.isd.wms.entity.Allocation;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import com.isd.wms.repository.projections.OperatorAllocationProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findAllByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);

    List<Allocation> findByStatus(Status status);

    @Query("""
            SELECT a FROM Allocation a
            JOIN Task t ON t = a.task
            WHERE t.operator = :operator
              AND a.status IN (:statuses)
            ORDER BY a.createdAt, a.id
        """)
    List<Allocation> findByOperatorAndStatuses(User operator, List<Status> statuses);

    @Query("""
            SELECT a FROM Allocation a
            JOIN Task t ON t = a.task
            JOIN User u ON u = t.operator
            WHERE u.username = :username
              AND a.status IN (:statuses)
            ORDER BY a.createdAt, a.id
        """)
    List<Allocation> findByOperatorUsernameAndStatuses(
        @Param("username") String username,
        @Param("statuses") List<Status> statuses
    );

    @Query(value = """
            with oldest_allocation as (select a.id as allocation_id from allocations a
                                           join tasks t on t.id = a.task_id
                                           join users u on t.operator_id = u.id
                                  where u.username = :userName
                                    and a.status in ('ASSIGNED', 'IN_PROGRESS')
                                  order by a.created_at, a.id
                                  limit 1)
        update allocations a
        set status = 'IN_PROGRESS'
        where a.id = (select allocation_id from oldest_allocation)
        returning a.id
        """, nativeQuery = true)
    Optional<Long> findOldestAssignedAllocationId(String username);

    Optional<Allocation> findFirstByTask_Operator_UsernameAndStatusInOrderByCreatedAtAscIdAsc(
        String username,
        List<Status> statuses
    );

    @Query("""
            SELECT a FROM Allocation a
            JOIN OrderLine o ON o.task.id = a.task.id
            WHERE o.order = :order
            GROUP BY a, o.order.id
            ORDER BY a.createdAt, a.id
        """)
    List<Allocation> findAllByOrder(
        @Param("order") Order order
    );

    @Query("""
            SELECT COUNT(DISTINCT a) FROM Allocation a
            JOIN OrderLine ol ON ol.task = a.task
            WHERE ol.order.id = :orderId
            AND NOT a.status = Status.CANCELED
        """)
    Integer countAllocationsInOrder(
        @Param("orderId") Long orderId
    );

    @Query("""
            SELECT COUNT(DISTINCT a) FROM Allocation a
            JOIN OrderLine ol ON ol.task = a.task
            WHERE ol.order.id = :orderId
            AND a.status = Status.COMPLETED
        """)
    Integer countCompletedAllocationsInOrder(
        @Param("orderId") Long orderId
    );

    @Query(value = """
            with oldest_order as (select o.id as order_id, o.logic_id as logic_id
                                  from orders o
                                           left join order_lines ol on ol.order_id = o.id
                                           left join tasks t on t.id = ol.task_id
                                           left join users u on t.operator_id = u.id
                                  where u.username = :userName
                                    and o.status in ('ASSIGNED', 'IN_PROGRESS')
                                  order by o.created_at, o.id
                                  limit 1)
            select oldest_order.order_id AS oldestOrderId,
                   oldest_order.logic_id AS orderName,
                   a.id AS allocationId,
                   pr.name AS productName,
                   pr.barcode AS productBarcode,
                   l.name AS locationName,
                   l.barcode AS locationBarcode, --rename it
                   a.quantity AS quantity
            from oldest_order
                     left join order_lines ol on ol.order_id = oldest_order.order_id
                     left join allocations a on a.task_id = ol.task_id
                     left join stocks s on a.stock_id = s.id
                     left join products pr on pr.id = s.product_id
                     left join locations l on l.id = s.location_id
            where a.status in ('ASSIGNED', 'IN_PROGRESS')
            order by a.created_at, a.id
            limit 1;
        """, nativeQuery = true)
    Optional<OperatorAllocationProjection> getAllocationInfoForOperator(
        @Param("userName") String currentUsername
    );

    @Modifying
    @Query("""
            UPDATE Allocation a
            SET a.status = :status
            WHERE a.task.id IN (
                SELECT ol.task.id
                FROM OrderLine ol
                WHERE ol.order.id = :orderId
            )
        """)
    int updateStatusByOrderId(
        @Param("orderId") Long orderId,
        @Param("status") Status status);

    @Query("""
        SELECT
            a.id AS allocationId,
            r.id AS replenishmentId,
            ol.order.id AS orderId,
            t.taskType AS type,
            a.stock.id AS stockId,
            a.stock.product.name AS productName,
            a.stock.location.name AS locationName,
            a.quantity AS quantity,
            a.status AS status,
            a.sourceLocationScanned AS sourceLocationScanned,
            a.productScanned AS productScanned,
            a.pickedQuantity AS pickedQuantity
        FROM Allocation a
        JOIN Task t ON a.task = t
        JOIN User u ON u = t.supervisor
        LEFT JOIN OrderLine ol ON ol.task = a.task
        LEFT JOIN Replenishment r ON r.task = a.task
        """)
    List<AllocationSupervisorProjection> getAllAllocations();
}
