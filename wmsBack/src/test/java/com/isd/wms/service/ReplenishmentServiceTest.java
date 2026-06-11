package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.Status;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplenishmentServiceTest {

    @Mock
    private ReplenishmentRepository replenishmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ReplenishmentMapper replenishmentMapper;
    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ReplenishmentService replenishmentService;

    private Product product;
    private Location destinationLocation;
    private User supervisor;
    private Task task;
    private Replenishment replenishment;
    private ReplenishmentResponse response;

    @BeforeEach
    void setUp() {
        product = mock(Product.class);
        destinationLocation = mock(Location.class);
        supervisor = mock(User.class);
        task = mock(Task.class);
        replenishment = mock(Replenishment.class);
        response = new ReplenishmentResponse(1L, 1L, 2L, 10, Status.CREATED, 3L, Timestamp.from(Instant.now()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String username) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createReplenishment_validRequest_returnsResponse() {
        mockSecurityContext("supervisor");
        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 3L);

        when(product.getId()).thenReturn(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(destinationLocation));
        when(userRepository.findByUsername("supervisor")).thenReturn(Optional.of(supervisor));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(replenishmentRepository.save(any(Replenishment.class))).thenReturn(replenishment);
        when(replenishmentMapper.toResponse(replenishment)).thenReturn(response);
        doNothing().when(workflowService).generateProcessesForTask(any(Task.class), eq(1L), eq(10));

        ReplenishmentResponse result = replenishmentService.createReplenishment(request);

        assertThat(result).isEqualTo(response);
        verify(taskRepository).save(any(Task.class));
        verify(replenishmentRepository).save(any(Replenishment.class));
        verify(workflowService).generateProcessesForTask(task, 1L, 10);
    }

    @Test
    void updateReplenishment_validRequest_returnsUpdatedResponse() {
        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 1L, 20, Status.COMPLETED, 3L);

        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(replenishment));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(destinationLocation));
        when(replenishmentRepository.save(replenishment)).thenReturn(replenishment);
        when(replenishmentMapper.toResponse(replenishment)).thenReturn(response);

        ReplenishmentResponse result = replenishmentService.updateReplenishment(1L, request);

        assertThat(result).isEqualTo(response);
        verify(replenishment).setProduct(product);
        verify(replenishment).setRequestedQuantity(20);
        verify(replenishment).setStatus(Status.COMPLETED);
        verify(replenishment).setDestinationLocation(destinationLocation);
    }

    @Test
    void searchReplenishments_withFilters_returnsMappedResults() {
        ReplenishmentSearchRequest request = new ReplenishmentSearchRequest(1L, null, null, Status.CREATED, null);

        when(replenishmentRepository.filter(1L, null, null, Status.CREATED, null))
                .thenReturn(List.of(replenishment));
        when(replenishmentMapper.toResponse(replenishment)).thenReturn(response);

        List<ReplenishmentResponse> result = replenishmentService.searchReplenishments(request);

        assertThat(result).hasSize(1).containsExactly(response);
    }
}
