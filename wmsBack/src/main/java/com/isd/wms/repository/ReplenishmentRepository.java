package com.isd.wms.repository;

import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.Task;
import com.isd.wms.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReplenishmentRepository extends JpaRepository<Replenishment, Long> {

    List<Replenishment> findByStatus(Status status);

    Optional<Replenishment> findByTaskId(Long taskId);

    @Query("""
            SELECT r FROM Replenishment r
            JOIN Task t ON t = r.task
            JOIN User u ON u = t.supervisor
            WHERE (:taskId IS NULL OR r.task.id = :taskId)
            AND (:productId IS NULL OR r.product.id = :productId)
            AND (:requestedQuantity IS NULL OR r.requestedQuantity = :requestedQuantity)
            AND (:status IS NULL OR r.status = :status)
            AND (:destinationLocationId IS NULL OR r.destinationLocation.id = :destinationLocationId)
            AND u.username = :username
            """)
    List<Replenishment> filter(
            @Param("username") String username,
            @Param("taskId") Long taskId,
            @Param("productId") Long productId,
            @Param("requestedQuantity") Integer requestedQuantity,
            @Param("status") Status status,
            @Param("destinationLocationId") Long destinationLocationId
    );

    @Query("""
        SELECT r FROM Replenishment r
        JOIN Task t ON t = r.task
        JOIN User u ON u = t.supervisor
        WHERE u.username = :username
        """)
    List<Replenishment> findAllByCreatedByUsername(@Param("username") String username);

    @Query("""
        SELECT r FROM Replenishment r
        JOIN Task t ON t = r.task
        JOIN User u ON u = t.supervisor
        WHERE r.id = :id AND u.username = :username
        """)
    Optional<Replenishment> findByIdAndCreatedByUsername(@Param("id") Long id, @Param("username") String username);

    boolean existsByProductIdAndDestinationLocationIdAndStatusNotIn(
            Long productId, Long destinationLocationId, Collection<Status> statuses
    );
    boolean existsByProductIdAndDestinationLocationIdAndStatusNotInAndIdNot(
            Long productId, Long destinationLocationId, Collection<Status> statuses, Long id
    );

    @Modifying
    @Query("""
        UPDATE Replenishment r
            SET r.status = Status.COMPLETED
            WHERE r.task = :task
    """)
    int updateReplenishmentStatusByTask(
        @Param("task") Task task
    );
}
