package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentResponse;
import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
import com.isd.wms.entity.*;
import com.isd.wms.enums.Status;
import com.isd.wms.mapper.ReplenishmentMapper;
import com.isd.wms.repository.*;
import com.isd.wms.service.validation.SecurityFacade;
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

import java.time.LocalDateTime;
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
    private ProductRepository productRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ReplenishmentMapper replenishmentMapper;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private TaskService taskService;
    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private ReplenishmentService replenishmentService;

    private Product product;
    private Location destinationLocation;
    private Task task;
    private Replenishment replenishment;
    private ReplenishmentResponse response;

    @BeforeEach
    void setUp() {
        product = new Product("Widget", "WGT-01", null, null);
        org.springframework.test.util.ReflectionTestUtils.setField(product, "id", 1L);

        destinationLocation = new Location("Pick Face", "PICK-01", null, null, true);
        org.springframework.test.util.ReflectionTestUtils.setField(destinationLocation, "id", 3L);

        task = new Task(null, com.isd.wms.enums.TaskType.REPLENISHMENT, 10);
        org.springframework.test.util.ReflectionTestUtils.setField(task, "id", 1L);

        replenishment = new Replenishment(task, product, 10, destinationLocation);
        org.springframework.test.util.ReflectionTestUtils.setField(replenishment, "id", 1L);

        response = new ReplenishmentResponse(1L, 1L, 2L, 10, Status.CREATED, 3L, LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReplenishment_validRequest_returnsResponse() {
        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 3L);

        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(destinationLocation));
        when(replenishmentRepository.existsByProductIdAndDestinationLocationIdAndStatusNotIn(eq(1L), eq(3L), any())).thenReturn(false);
        when(taskService.createTask(com.isd.wms.enums.TaskType.REPLENISHMENT, 10, 1L)).thenReturn(task);
        when(replenishmentRepository.save(any(Replenishment.class))).thenReturn(replenishment);
        when(replenishmentMapper.toResponse(replenishment)).thenReturn(response);

        ReplenishmentResponse result = replenishmentService.createReplenishment(request);

        assertThat(result).isEqualTo(response);
        verify(taskService).createTask(com.isd.wms.enums.TaskType.REPLENISHMENT, 10, 1L);
        verify(replenishmentRepository).save(any(Replenishment.class));
    }

    @Test
    void updateReplenishment_validRequest_returnsUpdatedResponse() {
        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 1L, 20, Status.COMPLETED, 3L);

        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(replenishmentRepository.findById(eq(1L))).thenReturn(Optional.of(replenishment));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(destinationLocation));
        when(replenishmentRepository.save(replenishment)).thenReturn(replenishment);
        when(replenishmentMapper.toResponse(replenishment)).thenReturn(response);

        ReplenishmentResponse result = replenishmentService.updateReplenishment(1L, request);

        assertThat(result).isEqualTo(response);
        assertThat(replenishment.getProduct()).isEqualTo(product);
        assertThat(replenishment.getRequestedQuantity()).isEqualTo(20);
        assertThat(replenishment.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(replenishment.getDestinationLocation()).isEqualTo(destinationLocation);
    }

    @Test
    void searchReplenishments_withFilters_returnsMappedResults() {
        ReplenishmentSearchRequest request = new ReplenishmentSearchRequest(1L, null, null, Status.CREATED, null);

        when(securityFacade.getCurrentUsername()).thenReturn("tester");
        when(replenishmentRepository.filter(any(), eq(1L), any(), any(), eq(Status.CREATED), any()))
                .thenReturn(List.of(replenishment));
        when(replenishmentMapper.toResponse(replenishment)).thenReturn(response);

        List<ReplenishmentResponse> result = replenishmentService.searchReplenishments(request);

        assertThat(result).hasSize(1).containsExactly(response);
    }
}
