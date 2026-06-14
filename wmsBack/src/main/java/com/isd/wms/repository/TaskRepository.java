package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("""
        SELECT t FROM Task t
        JOIN OrderLine ol ON ol.task = t
        WHERE ol.order = :order
    """)
    List<Task> findAllByOrder(
        @Param("order") Order order
    );

    @Modifying
    @Query(value = """
        WITH t_by_o AS (
            SELECT DISTINCT t.* FROM tasks t
            JOIN order_lines ol ON ol.task_id = t.id
            WHERE ol.order_id = :orderId)

        UPDATE tasks t
            SET operator_id = :operatorId
            WHERE id = ANY(SELECT * FROM t_by_o)
    """, nativeQuery = true)
    int updateOperatorByOrderId(
        @Param("orderId") Long orderId,
        @Param("operatorId") Long operatorId);
}
