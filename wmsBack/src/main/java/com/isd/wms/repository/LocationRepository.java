package com.isd.wms.repository;

import com.isd.wms.repository.projections.ShortLocationProjection;
import com.isd.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByBarcodeIgnoreCase(String barcode);

    List<Location> findAllByIsActiveTrue();

    boolean existsByNameIgnoreCase(String newCode);

    @Query("""
        SELECT l.id AS id, barcode AS barcode FROM Location l
        WHERE l.available = true
        AND l.isActive = true
        AND l.zone = com.isd.wms.enums.Zone.DISPATCH
        """)
    List<ShortLocationProjection> getLocationDispatch();

    @Query("""
            SELECT l.id FROM Location l
            WHERE l.name = :name
        """)
    Optional<Long> findLocationIdByName(
        @Param("name") String name
    );
}
