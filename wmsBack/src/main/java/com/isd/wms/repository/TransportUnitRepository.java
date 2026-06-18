package com.isd.wms.repository;

import com.isd.wms.entity.Order;
import com.isd.wms.entity.Replenishment;
import com.isd.wms.entity.TransportUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransportUnitRepository extends JpaRepository<TransportUnit, Long> {
    Optional<TransportUnit> findByBarcode(String barcode);

    boolean existsByOrder(Order order);

    boolean existsByReplenishment(Replenishment replenishment);

    Optional<TransportUnit> findByOrder(Order order);

    Optional<TransportUnit> findByReplenishment(Replenishment replenishment);
}
