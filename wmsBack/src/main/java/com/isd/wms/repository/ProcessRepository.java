package com.isd.wms.repository;

import com.isd.wms.dto.process.ProcessSupervisorProjection;
import com.isd.wms.repository.projections.OperatorProcessProjection;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessRepository extends JpaRepository<Process, Long> {
    List<Process> findAllByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);

    List<Process> findByStatus(Status status);

    @Query("""
        SELECT p FROM Process p
        JOIN Task t ON t = p.task
        WHERE t.operator = :operator
          AND p.status IN (:statuses)
        ORDER BY p.createdAt, p.id
    """)
    List<Process> findByOperatorAndStatuses(User operator, List<Status> statuses);

    @Query("""
        SELECT p FROM Process p
        JOIN Task t ON t = p.task
        JOIN User u ON u = t.operator
        WHERE u.username = :username
          AND p.status IN (:statuses)
        ORDER BY p.createdAt, p.id
    """)
    List<Process> findByOperatorUsernameAndStatuses(
        @Param("username") String username,
        @Param("statuses") List<Status> statuses
    );

    @Query(value = """
        with oldest_process as (select p.id as process_id from processes p
                                       join tasks t on t.id = p.task_id
                                       join users u on t.operator_id = u.id
                              where u.username = :userName
                                and p.status in ('ASSIGNED', 'IN_PROGRESS')
                              order by p.created_at, p.id
                              limit 1)
    update processes p
    set status = 'IN_PROGRESS'
    where p.id = (select process_id from oldest_process)
    returning p.id
    """, nativeQuery = true)
    Optional<Long> findOldestAssignedProcessId(String username);

    Optional<Process> findFirstByTask_Operator_UsernameAndStatusInOrderByCreatedAtAscIdAsc(
        String username,
        List<Status> statuses
    );

    @Query("""
        SELECT p FROM Process p
        JOIN OrderLine o ON o.task.id = p.task.id
        WHERE o.order = :order
        GROUP BY p, o.order.id
        ORDER BY p.createdAt, p.id
    """)
    List<Process> findAllByOrder(
        @Param("order") Order order
    );

    @Query("""
        SELECT COUNT(DISTINCT p) FROM Process p
        JOIN OrderLine ol ON ol.task = p.task
        WHERE ol.order.id = :orderId
        AND NOT p.status = Status.CANCELED
    """)
    Integer countProcessesInOrder(
        @Param("orderId") Long orderId
    );

    @Query("""
        SELECT COUNT(DISTINCT p) FROM Process p
        JOIN OrderLine ol ON ol.task = p.task
        WHERE ol.order.id = :orderId
        AND p.status = Status.COMPLETED
    """)
    Integer countCompletedProcessesInOrder(
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
               p.id AS processId,
               pr.name AS productName,
               pr.barcode AS productBarcode,
               l.name AS locationName,
               l.barcode AS locationBarcode, --rename it
               p.quantity AS quantity
        from oldest_order
                 left join order_lines ol on ol.order_id = oldest_order.order_id
                 left join processes p on p.task_id = ol.task_id
                 left join stocks s on p.stock_id = s.id
                 left join products pr on pr.id = s.product_id
                 left join locations l on l.id = s.location_id
        where p.status in ('ASSIGNED', 'IN_PROGRESS')
        order by p.created_at, p.id
        limit 1;
    """, nativeQuery = true)
    Optional<OperatorProcessProjection> getProcessInfoForOperator(
        @Param("userName") String currentUsername
    );

    @Modifying
    @Query("""
        UPDATE Process p
        SET p.status = :status
        WHERE p.task.id IN (
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
            p.id AS processId,
            r.id AS replenishmentId,
            ol.order.id AS orderId,
            t.taskType AS type,
            p.stock.id AS stockId,
            p.stock.product.name AS productName,
            p.stock.location.name AS locationName,
            p.quantity AS quantity,
            p.status AS status,
            p.sourceLocationScanned AS sourceLocationScanned,
            p.productScanned AS productScanned,
            p.pickedQuantity AS pickedQuantity
        FROM Process p
        JOIN Task t ON p.task = t
        JOIN User u ON u = t.supervisor
        LEFT JOIN OrderLine ol ON ol.task = p.task
        LEFT JOIN Replenishment r ON r.task = p.task
        WHERE u.username = :username
        """)
    List<ProcessSupervisorProjection> getAllProcessesSupervisor(
        @Param("username") String username
    );
}
