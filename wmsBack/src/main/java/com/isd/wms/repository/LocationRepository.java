package com.isd.wms.repository;

import com.isd.wms.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    boolean existsByLocationCodeIgnoreCase (String locationCode);

    List<Location> findAllByIsActiveTrue();

    Optional<Location> findByLocationCodeAndIsActiveTrue(String code);
}
