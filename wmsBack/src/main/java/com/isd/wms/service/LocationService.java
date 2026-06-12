package com.isd.wms.service;


import com.isd.wms.dto.location.LocationCreateRequest;
import com.isd.wms.dto.location.LocationResponse;
import com.isd.wms.dto.location.LocationUpdateRequest;
import com.isd.wms.dto.location.ShortLocationProjection;
import com.isd.wms.entity.Location;
import com.isd.wms.enums.Zone;
import com.isd.wms.exception.DuplicateLocationCodeException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.mapper.LocationMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final StockRepository stockRepository;


    @Transactional
    public LocationResponse createLocation(LocationCreateRequest request) {
        String code = request.locationCode().trim();
        if (locationRepository.existsByLocationCodeIgnoreCase(code)) {
            throw new DuplicateLocationCodeException(code);
        }


        Location location = new Location(
                code,
                request.zone(),
                request.description(),
                true
        );
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public LocationResponse updateLocation(Long locationId, LocationUpdateRequest request) {
        Location location = getLocation(locationId);
        String newCode = request.locationCode().trim();

        if (!location.getLocationCode().equalsIgnoreCase(newCode) &&
                locationRepository.existsByLocationCodeIgnoreCase(newCode)) {
            throw new DuplicateLocationCodeException(newCode);
        }

        boolean hasProducts = stockRepository.existsByLocationIdAndQuantityGreaterThan(locationId, 0);
        if (hasProducts) {
            log.warn("Attempt to edit occupied location ID: {}", locationId);
            throw new IllegalStateException("Нельзя изменить параметры локации, так как на ней физически находится товар. Сначала переместите товары в другую ячейку.");
        }

        location.setLocationCode(newCode);
        location.setZone(request.zone());
        location.setDescription(request.description());
        location.setAvailable(request.available());

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        boolean hasProducts = stockRepository.existsByLocationIdAndQuantityGreaterThan(locationId, 0);
        if (hasProducts) {
            log.warn("Attempt to deactivate occupied location ID: {}", locationId);
            throw new IllegalStateException("Нельзя деактивировать локацию, на ней находится товар.");
        }

        Location location = getLocation(locationId);
        location.setIsActive(false);

        locationRepository.save(location);

        log.info("Location ID {} successfully deactivated", locationId);
    }

    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAllByIsActiveTrue().stream()
            .map(locationMapper::toResponse)
            .toList();
    }

    public LocationResponse getLocationById(Long locationId) {
        return locationMapper.toResponse(getLocation(locationId));
    }

    public List<ShortLocationProjection> getShortLocationsDispatch() {
        return locationRepository.getLocationDispatch();
    }


    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
            .orElseThrow(() -> new LocationNotFoundException(locationId));
    }
}
