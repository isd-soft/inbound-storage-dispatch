package com.isd.wms.repository;

import com.isd.wms.entity.OrderLine;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findAllByOrderId(Long orderId);

    Optional<OrderLine> findByTaskId(Long taskId);

    @Modifying
    @Query("""
            UPDATE OrderLine ol
                SET ol.status = :status
                WHERE ol.order.id = :orderId
        """)
    int updateStatusByOrderId(
        @Param("orderId") Long orderId,
        @Param("status") Status status);

    @Modifying
    @Query("DELETE FROM OrderLine ol WHERE ol.order.createdAt < :cutoffDate")
    int deleteOrderLinesByOrderCreatedAtOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
