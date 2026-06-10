package com.isd.wms.repository;

import com.isd.wms.entity.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findAllByOrderId(Long orderId);

    Optional<OrderLine> findByTaskId(Long taskId);
}
