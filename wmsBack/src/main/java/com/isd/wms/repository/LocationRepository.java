package com.isd.wms.repository;

import com.isd.wms.dto.location.ShortLocationProjection;
import com.isd.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByBarcodeIgnoreCase(String barcode);

    List<Location> findAllByIsActiveTrue();

    @Query("""
        SELECT l.id AS id, barcode AS barcode FROM Location l
        WHERE l.available = true
        AND l.isActive = true
        AND l.zone = Zone.DISPATCH
        """)
    List<ShortLocationProjection> getLocationDispatch();
}
