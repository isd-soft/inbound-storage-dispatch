package com.isd.wms.service;

import com.isd.wms.dto.location.LocationCreateRequest;
import com.isd.wms.dto.location.LocationResponse;
import com.isd.wms.dto.location.LocationUpdateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.enums.Zone;
import com.isd.wms.exception.DuplicateLocationCodeException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.mapper.LocationMapper;
import com.isd.wms.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new LocationService(locationRepository, new LocationMapper());
    }

    @Test
    void createsLocationSuccessfully() {
        when(locationRepository.existsByLocationCodeIgnoreCase("A-01")).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocationResponse response = locationService.createLocation(
                new LocationCreateRequest("A-01", Zone.PICKING, "Main storage")
        );

        assertThat(response.locationCode()).isEqualTo("A-01");
        assertThat(response.zone()).isEqualTo(Zone.PICKING);
        assertThat(response.available()).isTrue();
    }

    @Test
    void rejectsCreationWhenLocationCodeExists() {
        when(locationRepository.existsByLocationCodeIgnoreCase("A-01")).thenReturn(true);

        assertThatThrownBy(() -> locationService.createLocation(new LocationCreateRequest("A-01", Zone.PICKING, null)))
                .isInstanceOf(DuplicateLocationCodeException.class)
                .hasMessageContaining("A-01");
    }

    @Test
    void getsLocationById() {
        when(locationRepository.findById(1L))
                .thenReturn(Optional.of(location(1L, "A-01", Zone.PICKING, true)));

        LocationResponse response = locationService.getLocationById(1L);

        assertThat(response.locationCode()).isEqualTo("A-01");
    }

    @Test
    void throwsExceptionWhenLocationNotFound() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getLocationById(99L))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void updatesLocationSuccessfullyWithoutChangingCode() {
        Location existingLocation = location(1L, "A-01", Zone.PICKING, true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.save(existingLocation)).thenReturn(existingLocation);

        LocationResponse response = locationService.updateLocation(
                1L,
                new LocationUpdateRequest("A-01", Zone.PICKING, "Updated desc", false)
        );

        assertThat(response.zone()).isEqualTo(Zone.PICKING);
        assertThat(response.available()).isFalse();
    }

    @Test
    void updatesLocationSuccessfullyWithNewCode() {
        Location existingLocation = location(1L, "A-01", Zone.PICKING, true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.existsByLocationCodeIgnoreCase("B-02")).thenReturn(false);
        when(locationRepository.save(existingLocation)).thenReturn(existingLocation);

        LocationResponse response = locationService.updateLocation(
                1L,
                new LocationUpdateRequest("B-02", Zone.REPLENISHMENT, "Desc", true)
        );

        assertThat(response.locationCode()).isEqualTo("B-02");
    }

    @Test
    void rejectsUpdateWhenNewLocationCodeExists() {
        Location existingLocation = location(1L, "A-01", Zone.PICKING, true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existingLocation));
        when(locationRepository.existsByLocationCodeIgnoreCase("B-02")).thenReturn(true);

        assertThatThrownBy(() -> locationService.updateLocation(1L, new LocationUpdateRequest("B-02", Zone.REPLENISHMENT, null, true)))
                .isInstanceOf(DuplicateLocationCodeException.class);
    }

    @Test
    void deletesLocationSuccessfully() {
        Location existingLocation = location(1L, "A-01", Zone.PICKING, true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(existingLocation));

        locationService.deleteLocation(1L);

        verify(locationRepository).delete(existingLocation);
    }

    @Test
    void getsAllLocations() {
        when(locationRepository.findAll()).thenReturn(List.of(
                location(1L, "A-01", Zone.PICKING, true),
                location(2L, "B-02", Zone.REPLENISHMENT, false)
        ));

        List<LocationResponse> responses = locationService.getAllLocations();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(LocationResponse::locationCode).containsExactly("A-01", "B-02");
    }

    private Location location(Long id, String code, Zone zone, boolean available) {
        Location location = new Location();
        location.setLocationCode(code);
        location.setZone(zone);
        location.setAvailable(available);
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }
}