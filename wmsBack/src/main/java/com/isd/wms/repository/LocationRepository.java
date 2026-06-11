package com.isd.wms.repository;

import com.isd.wms.dto.location.ShortLocationProjection;
import com.isd.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    boolean existsByLocationCodeIgnoreCase (String locationCode);

    List<Location> findAllByIsActiveTrue();

    Optional<Location> findByLocationCodeAndIsActiveTrue(String code);

    @Query("""
            SELECT l.id AS id, l.locationCode AS locationCode FROM Location l
            WHERE l.available = true
              AND l.isActive = true
              AND l.zone = com.isd.wms.enums.Zone.DISPATCH
            """)
    List<ShortLocationProjection> getLocationDispatch();
}
