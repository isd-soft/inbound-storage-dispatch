package com.isd.wms.repository;

import com.isd.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByLocationCodeIgnoreCase (String locationCode);
}
