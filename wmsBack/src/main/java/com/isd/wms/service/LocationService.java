package com.isd.wms.service;


import com.isd.wms.dto.location.LocationCreateRequest;
import com.isd.wms.dto.location.LocationResponse;
import com.isd.wms.dto.location.LocationUpdateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.exception.DuplicateBarcodeException;
import com.isd.wms.exception.DuplicateLocationNameException;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.mapper.LocationMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.projections.ShortLocationProjection;
import com.isd.wms.service.imports.ImportService;
import com.isd.wms.service.imports.dto.LocationInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class LocationService {
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final StockRepository stockRepository;
    private final ImportService importService;


    @Transactional
    public LocationResponse createLocation(LocationCreateRequest request) {
        String code = request.barcode().trim();
        String name = request.name().trim();

        if (locationRepository.existsByBarcodeIgnoreCase(code)) {
            throw new DuplicateBarcodeException(code);
        }

        if (locationRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateLocationNameException(name);
        }

        System.out.println(name + " " + code);
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
        String newName = request.name().trim();
        String newCode = request.barcode().trim();

        boolean isNameChanged = !location.getName().equalsIgnoreCase(newName);
        boolean isCodeChanged = !location.getBarcode().equalsIgnoreCase(newCode);
        boolean isZoneChanged = location.getZone() != request.zone();
        boolean isAvailableChanged = location.getAvailable() != request.available();

        boolean hasProducts = stockRepository.existsByLocationIdAndQuantityGreaterThan(locationId, 0);

        if (hasProducts && (isNameChanged || isCodeChanged || isZoneChanged || isAvailableChanged)) {
            log.warn("Attempt to edit protected fields of occupied location ID: {}", locationId);
            throw new IllegalStateException("Cannot change the code, zone, or availability of a location that contains products. Only the description can be updated. Please move the products first.");
        }

        if (isCodeChanged && locationRepository.existsByBarcodeIgnoreCase(newCode)) {
            throw new DuplicateBarcodeException(newCode);
        }

        if (isNameChanged && locationRepository.existsByNameIgnoreCase(newCode)) {
            throw new DuplicateLocationNameException(newName);
        }

        location.setName(newName);
        location.setBarcode(newCode);
        location.setZone(request.zone());
        location.setDescription(request.description());
        location.setAvailable(request.available());

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        boolean hasProducts = stockRepository.existsByLocationIdAndQuantityGreaterThan(locationId, 0);
        if (hasProducts) {
            log.warn("Attempt to delete occupied location ID: {}", locationId);
            throw new IllegalStateException("You cannot delete a location while there is a product in it");
        }

        Location location = getLocation(locationId);
        location.setIsActive(false);

        locationRepository.save(location);

        log.info("Location ID {} successfully deleted", locationId);
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

    @Transactional
    public void importLocationsFromFile(MultipartFile file) {
        List<LocationCreateRequest> locations = importService.importData(file, LocationInfo.class);
        try {
            locations.forEach(this::createLocation);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("The imported file contains invalid location data.");
        }
    }
}
