package com.isd.wms.service;

import com.isd.wms.dto.location.LocationCreateRequest;
import com.isd.wms.dto.location.LocationResponse;
import com.isd.wms.dto.location.LocationUpdateRequest;
import com.isd.wms.dto.location.ShortLocationProjection;
import com.isd.wms.entity.Location;
import com.isd.wms.exception.DuplicateBarcodeException;
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
        String name = request.name().trim();
        String code = request.barcode().trim();

        log.info("Attempting to create a new warehouse location: Name='{}', Barcode='{}', Zone='{}'", name, code, request.zone());

        if (locationRepository.existsByBarcodeIgnoreCase(code)) {
            log.warn("Location creation rejected: Barcode '{}' already exists in the system", code);
            throw new DuplicateBarcodeException(code);
        }

        Location location = new Location(
                name,
                code,
                request.zone(),
                request.description(),
                true
        );

        Location savedLocation = locationRepository.save(location);
        log.info("Location successfully created. Assigned ID: {}, Barcode: '{}'", savedLocation.getId(), code);

        return locationMapper.toResponse(savedLocation);
    }

    @Transactional
    public LocationResponse updateLocation(Long locationId, LocationUpdateRequest request) {
        Location location = getLocation(locationId);
        String newCode = request.barcode().trim();

        log.info("Updating warehouse Location ID: {}. Old Barcode='{}', New Barcode='{}', Zone='{}'",
            locationId, location.getBarcode(), newCode, request.zone());

        if (!location.getBarcode().equalsIgnoreCase(newCode) &&
                locationRepository.existsByBarcodeIgnoreCase(newCode)) {
            log.warn("Location update rejected for ID {}: Barcode '{}' is already assigned to another location", locationId, newCode);
            throw new DuplicateBarcodeException(newCode);
        }

        location.setBarcode(newCode);
        location.setZone(request.zone());
        location.setDescription(request.description());
        location.setAvailable(request.available());

        Location updatedLocation = locationRepository.save(location);
        log.info("Location ID {} successfully updated in database", locationId);

        return locationMapper.toResponse(updatedLocation);
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        log.info("Delete (deactivation) requested for Location ID: {}", locationId);

        boolean hasProducts = stockRepository.existsByLocationIdAndQuantityGreaterThan(locationId, 0);
        if (hasProducts) {
            log.warn("Attempt to deactivate occupied location ID: {}", locationId);
            throw new IllegalStateException("You cannot deactivate this location, there is a product on it.");
        }

        Location location = getLocation(locationId);
        location.setIsActive(false); // Deactivate

        locationRepository.save(location);

        log.warn("Location ID {} ('{}') has been successfully deactivated (marked as inactive)", locationId, location.getName());
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
            .orElseThrow(() -> {
                log.warn("Location lookup failed. Warehouse Location ID {} not found", locationId);
                return new LocationNotFoundException(locationId);
            });
    }
}
