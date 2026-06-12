package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
        AND t.operator = :operator
    """)
    List<Task> findAllByOrder(
        @Param("order") Order order);

//    @Query("""
//        SELECT t FROM Task t
//        JOIN OrderLine ol ON ol.task = t
//        WHERE ol.order = :order
//        AND t.operator = :operator
//    """)
//    List<Task> findAllByOrder(
//        @Param("order") Order order,
//        @Param("operator") User operator
//    );
}
