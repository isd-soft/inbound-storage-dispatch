//package com.isd.wms.service;
//
//import com.isd.wms.dto.replenishment.ReplenishmentCreateRequest;
//import com.isd.wms.dto.replenishment.ReplenishmentResponse;
//import com.isd.wms.dto.replenishment.ReplenishmentSearchRequest;
//import com.isd.wms.dto.replenishment.ReplenishmentUpdateRequest;
//import com.isd.wms.entity.Location;
//import com.isd.wms.entity.Product;
//import com.isd.wms.entity.Replenishment;
//import com.isd.wms.entity.User;
//import com.isd.wms.enums.ReplenishmentStatus;
//import com.isd.wms.exception.InvalidRequestException;
//import com.isd.wms.exception.ProductNotFoundException;
//import com.isd.wms.exception.ReplenishmentNotFoundException;
//import com.isd.wms.exception.UserNotFoundException;
//import com.isd.wms.mapper.ReplenishmentMapper;
//import com.isd.wms.repository.LocationRepository;
//import com.isd.wms.repository.ProductRepository;
//import com.isd.wms.repository.ReplenishmentRepository;
//import com.isd.wms.repository.UserRepository;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.sql.Timestamp;
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ReplenishmentServiceTest {
//
//    @Mock
//    private ReplenishmentRepository replenishmentRepository;
//    @Mock
//    private ProductRepository productRepository;
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private LocationRepository locationRepository;
//    @Mock
//    private ReplenishmentMapper replenishmentMapper;
//
//    @InjectMocks
//    private ReplenishmentService replenishmentService;
//
//    private Product product;
//    private Location sourceLocation;
//    private Location destinationLocation;
//    private User operator;
//    private Replenishment task;
//    private ReplenishmentResponse response;
//
//    @BeforeEach
//    void setUp() {
//        product = mock(Product.class);
//        sourceLocation = mock(Location.class);
//        destinationLocation = mock(Location.class);
//        operator = mock(User.class);
//        task = mock(Replenishment.class);
//        response = new ReplenishmentResponse(1L, 1L, 2L, 10, ReplenishmentStatus.CREATED, 3L, 4L, Timestamp.from(Instant.now()));
//    }
//
//    @AfterEach
//    void tearDown() {
//        SecurityContextHolder.clearContext();
//    }
//
//    private void mockSecurityContext(String username) {
//        Authentication authentication = mock(Authentication.class);
//        SecurityContext securityContext = mock(SecurityContext.class);
//        when(authentication.getName()).thenReturn(username);
//        when(securityContext.getAuthentication()).thenReturn(authentication);
//        SecurityContextHolder.setContext(securityContext);
//    }
//
//    @Test
//    void createReplenishment_validRequest_returnsResponse() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 3L, 4L);
//
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(locationRepository.findById(3L)).thenReturn(Optional.of(sourceLocation));
//        when(locationRepository.findById(4L)).thenReturn(Optional.of(destinationLocation));
//        when(replenishmentRepository.save(any(Replenishment.class))).thenReturn(task);
//        when(replenishmentMapper.toResponse(task)).thenReturn(response);
//
//        ReplenishmentResponse result = replenishmentService.createReplenishment(request);
//
//        assertThat(result).isEqualTo(response);
//        verify(replenishmentRepository).save(any(Replenishment.class));
//    }
//
//    @Test
//    void createReplenishment_nullProductId_throwsInvalidRequestException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(null, 10, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("product id is required");
//    }
//
//    @Test
//    void createReplenishment_nullRequestedQuantity_throwsInvalidRequestException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, null, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("requested quantity is required");
//    }
//
//    @Test
//    void createReplenishment_zeroRequestedQuantity_throwsInvalidRequestException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 0, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("requested quantity cannot be nonpositive");
//    }
//
//    @Test
//    void createReplenishment_negativeRequestedQuantity_throwsInvalidRequestException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, -5, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("requested quantity cannot be nonpositive");
//    }
//
//    @Test
//    void createReplenishment_nullSourceLocationId_throwsInvalidRequestException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, null, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("source location id is required");
//    }
//
//    @Test
//    void createReplenishment_nullDestinationLocationId_throwsInvalidRequestException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 3L, null);
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("destination location id is required");
//    }
//
//    @Test
//    void createReplenishment_productNotFound_throwsProductNotFoundException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(99L, 10, 3L, 4L);
//        when(productRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(ProductNotFoundException.class);
//    }
//
//    @Test
//    void createReplenishment_sourceLocationNotFound_throwsProductNotFoundException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 99L, 4L);
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(locationRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(ProductNotFoundException.class);
//    }
//
//    @Test
//    void createReplenishment_destinationLocationNotFound_throwsProductNotFoundException() {
//        ReplenishmentCreateRequest request = new ReplenishmentCreateRequest(1L, 10, 3L, 99L);
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(locationRepository.findById(3L)).thenReturn(Optional.of(sourceLocation));
//        when(locationRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.createReplenishment(request))
//                .isInstanceOf(ProductNotFoundException.class);
//    }
//
//    @Test
//    void updateReplenishment_validRequest_returnsUpdatedResponse() {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 2L, 20, ReplenishmentStatus.ASSIGNED, 3L, 4L);
//
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(userRepository.findById(2L)).thenReturn(Optional.of(operator));
//        when(locationRepository.findById(3L)).thenReturn(Optional.of(sourceLocation));
//        when(locationRepository.findById(4L)).thenReturn(Optional.of(destinationLocation));
//        when(replenishmentRepository.save(task)).thenReturn(task);
//        when(replenishmentMapper.toResponse(task)).thenReturn(response);
//
//        ReplenishmentResponse result = replenishmentService.updateReplenishment(1L, request);
//
//        assertThat(result).isEqualTo(response);
//        verify(task).setProduct(product);
//        verify(task).setOperator(operator);
//        verify(task).setRequestedQuantity(20);
//        verify(task).setStatus(ReplenishmentStatus.ASSIGNED);
//        verify(task).setSourceLocation(sourceLocation);
//        verify(task).setDestinationLocation(destinationLocation);
//    }
//
//    @Test
//    void updateReplenishmentTask_taskNotFound_throwsReplenishmentNotFoundException() {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 2L, 20, ReplenishmentStatus.ASSIGNED, 3L, 4L);
//        when(replenishmentRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.updateReplenishment(99L, request))
//                .isInstanceOf(ReplenishmentNotFoundException.class);
//    }
//
//    @Test
//    void updateReplenishment_nullProductId_throwsInvalidRequestException() {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(null, 2L, 20, ReplenishmentStatus.ASSIGNED, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.updateReplenishment(1L, request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("product id is required");
//    }
//
//    @Test
//    void updateReplenishment_nullOperatorId_throwsInvalidRequestException() {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, null, 20, ReplenishmentStatus.ASSIGNED, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.updateReplenishment(1L, request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("operator id is required");
//    }
//
//    @Test
//    void updateReplenishment_nonPositiveQuantity_throwsInvalidRequestException() {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 2L, 0, ReplenishmentStatus.ASSIGNED, 3L, 4L);
//
//        assertThatThrownBy(() -> replenishmentService.updateReplenishment(1L, request))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("requested quantity cannot be nonpositive");
//    }
//
//    @Test
//    void updateReplenishment_operatorNotFound_throwsUserNotFoundException() {
//        ReplenishmentUpdateRequest request = new ReplenishmentUpdateRequest(1L, 99L, 20, ReplenishmentStatus.ASSIGNED, 3L, 4L);
//
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(userRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.updateReplenishment(1L, request))
//                .isInstanceOf(UserNotFoundException.class);
//    }
//
//    @Test
//    void deleteReplenishmentTask_existing_deletesSuccessfully() {
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//
//        replenishmentService.deleteReplenishment(1L);
//
//        verify(replenishmentRepository).delete(task);
//    }
//
//    @Test
//    void deleteReplenishmentTask_taskNotFound_throwsReplenishmentNotFoundException() {
//        when(replenishmentRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.deleteReplenishment(99L))
//                .isInstanceOf(ReplenishmentNotFoundException.class);
//
//        verify(replenishmentRepository, never()).delete(any());
//    }
//
//    @Test
//    void assignReplenishment_validRequest_returnsAssignedResponse() {
//        mockSecurityContext("john");
//        when(task.getStatus()).thenReturn(ReplenishmentStatus.CREATED);
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//        when(userRepository.findByUsername("john")).thenReturn(Optional.of(operator));
//        when(replenishmentRepository.save(task)).thenReturn(task);
//        when(replenishmentMapper.toResponse(task)).thenReturn(response);
//
//        ReplenishmentResponse result = replenishmentService.assignReplenishment(1L);
//
//        assertThat(result).isEqualTo(response);
//        verify(task).setStatus(ReplenishmentStatus.ASSIGNED);
//        verify(task).setOperator(operator);
//    }
//
//    @Test
//    void assignReplenishmentTask_taskNotFound_throwsReplenishmentNotFoundException() {
//        when(replenishmentRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.assignReplenishment(99L))
//                .isInstanceOf(ReplenishmentNotFoundException.class);
//    }
//
//    @Test
//    void assignReplenishment_statusNotCreated_throwsInvalidRequestException() {
//        mockSecurityContext("john");
//        when(task.getStatus()).thenReturn(ReplenishmentStatus.ASSIGNED);
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//
//        assertThatThrownBy(() -> replenishmentService.assignReplenishment(1L))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("status must be CREATED");
//    }
//
//    @Test
//    void assignReplenishment_userNotFound_throwsUserNotFoundException() {
//        mockSecurityContext("unknown");
//        when(task.getStatus()).thenReturn(ReplenishmentStatus.CREATED);
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.assignReplenishment(1L))
//                .isInstanceOf(UserNotFoundException.class);
//    }
//
//    @Test
//    void getReplenishmentById_existingId_returnsResponse() {
//        when(replenishmentRepository.findById(1L)).thenReturn(Optional.of(task));
//        when(replenishmentMapper.toResponse(task)).thenReturn(response);
//
//        ReplenishmentResponse result = replenishmentService.getReplenishmentById(1L);
//
//        assertThat(result).isEqualTo(response);
//    }
//
//    @Test
//    void getReplenishmentTaskById_notFound_throwsReplenishmentNotFoundException() {
//        when(replenishmentRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> replenishmentService.getReplenishmentById(99L))
//                .isInstanceOf(ReplenishmentNotFoundException.class);
//    }
//
//    @Test
//    void getAllReplenishments_returnsMappedList() {
//        Replenishment task2 = mock(Replenishment.class);
//        ReplenishmentResponse response2 = new ReplenishmentResponse(2L, 2L, null, 5, ReplenishmentStatus.ASSIGNED, 5L, 6L, null);
//
//        when(replenishmentRepository.findAll()).thenReturn(List.of(task, task2));
//        when(replenishmentMapper.toResponse(task)).thenReturn(response);
//        when(replenishmentMapper.toResponse(task2)).thenReturn(response2);
//
//        List<ReplenishmentResponse> result = replenishmentService.getAllReplenishments();
//
//        assertThat(result).hasSize(2).containsExactly(response, response2);
//    }
//
//    @Test
//    void getAllReplenishments_emptyRepository_returnsEmptyList() {
//        when(replenishmentRepository.findAll()).thenReturn(List.of());
//
//        List<ReplenishmentResponse> result = replenishmentService.getAllReplenishments();
//
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    void searchReplenishments_withFilters_returnsMappedResults() {
//        ReplenishmentSearchRequest request = new ReplenishmentSearchRequest(1L, null, null, ReplenishmentStatus.CREATED, null, null);
//
//        when(replenishmentRepository.filter(1L, null, null, ReplenishmentStatus.CREATED, null, null))
//                .thenReturn(List.of(task));
//        when(replenishmentMapper.toResponse(task)).thenReturn(response);
//
//        List<ReplenishmentResponse> result = replenishmentService.searchReplenishments(request);
//
//        assertThat(result).hasSize(1).containsExactly(response);
//    }
//
//    @Test
//    void searchReplenishments_noMatches_returnsEmptyList() {
//        ReplenishmentSearchRequest request = new ReplenishmentSearchRequest(999L, null, null, null, null, null);
//
//        when(replenishmentRepository.filter(999L, null, null, null, null, null))
//                .thenReturn(List.of());
//
//        List<ReplenishmentResponse> result = replenishmentService.searchReplenishments(request);
//
//        assertThat(result).isEmpty();
//    }
//
//    @Test
//    void getCurrentUsername_returnsUsernameFromSecurityContext() {
//        mockSecurityContext("testUser");
//
//        String username = replenishmentService.getCurrentUsername();
//
//        assertThat(username).isEqualTo("testUser");
//    }
//
//    @Test
//    void validateReplenishmentRequest_nullUsername_throwsInvalidRequestException() {
//        assertThatThrownBy(() -> replenishmentService.validateReplenishmentRequest(null, ReplenishmentStatus.CREATED))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("Username is required");
//    }
//
//    @Test
//    void validateReplenishmentRequest_emptyUsername_throwsInvalidRequestException() {
//        assertThatThrownBy(() -> replenishmentService.validateReplenishmentRequest("", ReplenishmentStatus.CREATED))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("Username is required");
//    }
//
//    @Test
//    void validateReplenishmentRequest_statusNotCreated_throwsInvalidRequestException() {
//        assertThatThrownBy(() -> replenishmentService.validateReplenishmentRequest("john", ReplenishmentStatus.ASSIGNED))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessageContaining("status must be CREATED");
//    }
//
//    @Test
//    void validateReplenishmentRequest_validInput_doesNotThrow() {
//        replenishmentService.validateReplenishmentRequest("john", ReplenishmentStatus.CREATED);
//    }
//}