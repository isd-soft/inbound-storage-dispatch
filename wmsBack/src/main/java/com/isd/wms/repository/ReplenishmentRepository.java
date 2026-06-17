package com.isd.wms.repository;

import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReplenishmentRepository extends JpaRepository<Replenishment, Long> {

    List<Replenishment> findByStatus(Status status);

    Optional<Replenishment> findByTaskId(Long taskId);

    @Query("""
        SELECT r FROM Replenishment r
        LEFT JOIN r.task t
        WHERE (:taskId IS NULL OR r.task.id = :taskId)
        AND (:productId IS NULL OR r.product.id = :productId)
        AND (:requestedQuantity IS NULL OR r.requestedQuantity = :requestedQuantity)
        AND (:status IS NULL OR r.status = :status)
        AND (:destinationLocationId IS NULL OR r.destinationLocation.id = :destinationLocationId)
        """)
    List<Replenishment> filter(
        @Param("taskId") Long taskId,
        @Param("productId") Long productId,
        @Param("requestedQuantity") Integer requestedQuantity,
        @Param("status") Status status,
        @Param("destinationLocationId") Long destinationLocationId
    );

    @Query("""
        SELECT r FROM Replenishment r
        JOIN r.task t
        JOIN t.supervisor u
        WHERE u.username = :username
        """)
    List<Replenishment> findAllByCreatedByUsername(@Param("username") String username);

    boolean existsByProductIdAndDestinationLocationIdAndStatusIn(
        Long productId, Long destinationLocationId, Collection<Status> statuses
    );

    @Modifying
    @Query("""
            UPDATE Replenishment r
                SET r.status = com.isd.wms.enums.Status.COMPLETED
                WHERE r.task = :task
        """)
    int updateReplenishmentStatusByTask(
        @Param("task") Task task
    );

    @Modifying
    @Query("DELETE FROM Replenishment r WHERE r.task.createdAt < :cutoffDate")
    int deleteReplenishmentsByTaskCreatedAtOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
