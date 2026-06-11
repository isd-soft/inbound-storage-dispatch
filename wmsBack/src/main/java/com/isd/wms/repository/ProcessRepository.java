package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessRepository extends JpaRepository<Process, Long> {
    List<Process> findAllByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);

    List<Process> findByStatus(Status status);

    @Query("SELECT p FROM Process p WHERE p.operator = :operator AND p.status IN (:statuses)")
    List<Process> findByOperatorAndStatuses(User operator, List<Status> statuses);

    @Query("""
        SELECT p FROM Process p
        JOIN OrderLine o ON o.task.id = p.task.id
        WHERE o.order = :order
        GROUP BY p, o.order.id
        ORDER BY p.createdAt
    """)
    List<Process> findAllByOrder(
        @Param("order") Order order
    );

    @Query("""
        SELECT p  FROM Process p
        JOIN OrderLine o ON o.task.id = p.task.id
        WHERE o.order = :order AND p.operator.id = :operator
        GROUP BY p, o.order.id
        ORDER BY p.createdAt
    """)
    List<Process> findOldestProcessesByOrder(
        @Param("order") Order order,
        @Param("operator") User operator
    );

}
