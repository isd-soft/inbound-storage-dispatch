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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    public LocationService(LocationRepository locationRepository, LocationMapper locationMapper) {
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
    }

    @Transactional
    public LocationResponse createLocation(LocationCreateRequest request) {
        String name = request.name().trim();
        String code = request.barcode().trim();
        if (locationRepository.existsByBarcodeIgnoreCase(code)) {
            throw new DuplicateBarcodeException(code);
        }

        Location location = new Location(
                name,
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
        String newCode = request.barcode().trim();

        if (!location.getBarcode().equalsIgnoreCase(newCode) &&
                locationRepository.existsByBarcodeIgnoreCase(newCode)) {
            throw new DuplicateBarcodeException(newCode);
        }

        location.setBarcode(newCode);
        location.setZone(request.zone());
        location.setDescription(request.description());
        location.setAvailable(request.available());

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        locationRepository.delete(getLocation(locationId));
    }

    public LocationResponse getLocationById(Long locationId) {
        return locationMapper.toResponse(getLocation(locationId));
    }

    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    public List<ShortLocationProjection> getShortLocationsDispatch() {
        return locationRepository.getLocationDispatch();
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationNotFoundException(locationId));
    }
}
