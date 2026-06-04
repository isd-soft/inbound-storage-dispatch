package com.isd.wms.service;

import com.isd.wms.dto.replenishment.ReplenishmentTaskCreateRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskResponse;
import com.isd.wms.dto.replenishment.ReplenishmentTaskSearchRequest;
import com.isd.wms.dto.replenishment.ReplenishmentTaskUpdateRequest;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.ReplenishmentTask;
import com.isd.wms.entity.User;
import com.isd.wms.enums.ReplenishmentTaskStatus;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.ReplenishmentTaskNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.ReplenishmentTaskMapper;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.ReplenishmentTaskRepository;
import com.isd.wms.repository.UserRepository;
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
class ReplenishmentTaskServiceTest {

    @Mock
    private ReplenishmentTaskRepository replenishmentTaskRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private ReplenishmentTaskMapper replenishmentTaskMapper;

    @InjectMocks
    private ReplenishmentTaskService replenishmentTaskService;

    private Product product;
    private Location sourceLocation;
    private Location destinationLocation;
    private User operator;
    private ReplenishmentTask task;
    private ReplenishmentTaskResponse response;

    @BeforeEach
    void setUp() {
        product = mock(Product.class);
        sourceLocation = mock(Location.class);
        destinationLocation = mock(Location.class);
        operator = mock(User.class);
        task = mock(ReplenishmentTask.class);
        response = new ReplenishmentTaskResponse(1L, 1L, 2L, 10, ReplenishmentTaskStatus.CREATED, 3L, 4L, Timestamp.from(Instant.now()));
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
    void createReplenishmentTask_validRequest_returnsResponse() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 3L, 4L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(sourceLocation));
        when(locationRepository.findById(4L)).thenReturn(Optional.of(destinationLocation));
        when(replenishmentTaskRepository.save(any(ReplenishmentTask.class))).thenReturn(task);
        when(replenishmentTaskMapper.toResponse(task)).thenReturn(response);

        ReplenishmentTaskResponse result = replenishmentTaskService.createReplenishmentTask(request);

        assertThat(result).isEqualTo(response);
        verify(replenishmentTaskRepository).save(any(ReplenishmentTask.class));
    }

    @Test
    void createReplenishmentTask_nullProductId_throwsInvalidRequestException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(null, 10, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("product id is required");
    }

    @Test
    void createReplenishmentTask_nullRequestedQuantity_throwsInvalidRequestException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, null, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("requested quantity is required");
    }

    @Test
    void createReplenishmentTask_zeroRequestedQuantity_throwsInvalidRequestException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 0, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("requested quantity cannot be nonpositive");
    }

    @Test
    void createReplenishmentTask_negativeRequestedQuantity_throwsInvalidRequestException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, -5, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("requested quantity cannot be nonpositive");
    }

    @Test
    void createReplenishmentTask_nullSourceLocationId_throwsInvalidRequestException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, null, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("source location id is required");
    }

    @Test
    void createReplenishmentTask_nullDestinationLocationId_throwsInvalidRequestException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 3L, null);

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("destination location id is required");
    }

    @Test
    void createReplenishmentTask_productNotFound_throwsProductNotFoundException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(99L, 10, 3L, 4L);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createReplenishmentTask_sourceLocationNotFound_throwsProductNotFoundException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 99L, 4L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createReplenishmentTask_destinationLocationNotFound_throwsProductNotFoundException() {
        ReplenishmentTaskCreateRequest request = new ReplenishmentTaskCreateRequest(1L, 10, 3L, 99L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(sourceLocation));
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.createReplenishmentTask(request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void updateReplenishmentTask_validRequest_returnsUpdatedResponse() {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, 2L, 20, ReplenishmentTaskStatus.ASSIGNED, 3L, 4L);

        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(2L)).thenReturn(Optional.of(operator));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(sourceLocation));
        when(locationRepository.findById(4L)).thenReturn(Optional.of(destinationLocation));
        when(replenishmentTaskRepository.save(task)).thenReturn(task);
        when(replenishmentTaskMapper.toResponse(task)).thenReturn(response);

        ReplenishmentTaskResponse result = replenishmentTaskService.updateReplenishmentTask(1L, request);

        assertThat(result).isEqualTo(response);
        verify(task).setProduct(product);
        verify(task).setOperator(operator);
        verify(task).setRequestedQuantity(20);
        verify(task).setStatus(ReplenishmentTaskStatus.ASSIGNED);
        verify(task).setSourceLocation(sourceLocation);
        verify(task).setDestinationLocation(destinationLocation);
    }

    @Test
    void updateReplenishmentTask_taskNotFound_throwsReplenishmentTaskNotFoundException() {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, 2L, 20, ReplenishmentTaskStatus.ASSIGNED, 3L, 4L);
        when(replenishmentTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.updateReplenishmentTask(99L, request))
                .isInstanceOf(ReplenishmentTaskNotFoundException.class);
    }

    @Test
    void updateReplenishmentTask_nullProductId_throwsInvalidRequestException() {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(null, 2L, 20, ReplenishmentTaskStatus.ASSIGNED, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.updateReplenishmentTask(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("product id is required");
    }

    @Test
    void updateReplenishmentTask_nullOperatorId_throwsInvalidRequestException() {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, null, 20, ReplenishmentTaskStatus.ASSIGNED, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.updateReplenishmentTask(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("operator id is required");
    }

    @Test
    void updateReplenishmentTask_nonPositiveQuantity_throwsInvalidRequestException() {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, 2L, 0, ReplenishmentTaskStatus.ASSIGNED, 3L, 4L);

        assertThatThrownBy(() -> replenishmentTaskService.updateReplenishmentTask(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("requested quantity cannot be nonpositive");
    }

    @Test
    void updateReplenishmentTask_operatorNotFound_throwsUserNotFoundException() {
        ReplenishmentTaskUpdateRequest request = new ReplenishmentTaskUpdateRequest(1L, 99L, 20, ReplenishmentTaskStatus.ASSIGNED, 3L, 4L);

        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.updateReplenishmentTask(1L, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deleteReplenishmentTask_existingTask_deletesSuccessfully() {
        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        replenishmentTaskService.deleteReplenishmentTask(1L);

        verify(replenishmentTaskRepository).delete(task);
    }

    @Test
    void deleteReplenishmentTask_taskNotFound_throwsReplenishmentTaskNotFoundException() {
        when(replenishmentTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.deleteReplenishmentTask(99L))
                .isInstanceOf(ReplenishmentTaskNotFoundException.class);

        verify(replenishmentTaskRepository, never()).delete(any());
    }

    @Test
    void assignReplenishmentTask_validRequest_returnsAssignedResponse() {
        mockSecurityContext("john");
        when(task.getStatus()).thenReturn(ReplenishmentTaskStatus.CREATED);
        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(operator));
        when(replenishmentTaskRepository.save(task)).thenReturn(task);
        when(replenishmentTaskMapper.toResponse(task)).thenReturn(response);

        ReplenishmentTaskResponse result = replenishmentTaskService.assignReplenishmentTask(1L);

        assertThat(result).isEqualTo(response);
        verify(task).setStatus(ReplenishmentTaskStatus.ASSIGNED);
        verify(task).setOperator(operator);
    }

    @Test
    void assignReplenishmentTask_taskNotFound_throwsReplenishmentTaskNotFoundException() {
        when(replenishmentTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.assignReplenishmentTask(99L))
                .isInstanceOf(ReplenishmentTaskNotFoundException.class);
    }

    @Test
    void assignReplenishmentTask_statusNotCreated_throwsInvalidRequestException() {
        mockSecurityContext("john");
        when(task.getStatus()).thenReturn(ReplenishmentTaskStatus.ASSIGNED);
        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> replenishmentTaskService.assignReplenishmentTask(1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("status must be CREATED");
    }

    @Test
    void assignReplenishmentTask_userNotFound_throwsUserNotFoundException() {
        mockSecurityContext("unknown");
        when(task.getStatus()).thenReturn(ReplenishmentTaskStatus.CREATED);
        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.assignReplenishmentTask(1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getReplenishmentTaskById_existingId_returnsResponse() {
        when(replenishmentTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(replenishmentTaskMapper.toResponse(task)).thenReturn(response);

        ReplenishmentTaskResponse result = replenishmentTaskService.getReplenishmentTaskById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getReplenishmentTaskById_notFound_throwsReplenishmentTaskNotFoundException() {
        when(replenishmentTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replenishmentTaskService.getReplenishmentTaskById(99L))
                .isInstanceOf(ReplenishmentTaskNotFoundException.class);
    }

    @Test
    void getAllReplenishmentTasks_returnsMappedList() {
        ReplenishmentTask task2 = mock(ReplenishmentTask.class);
        ReplenishmentTaskResponse response2 = new ReplenishmentTaskResponse(2L, 2L, null, 5, ReplenishmentTaskStatus.ASSIGNED, 5L, 6L, null);

        when(replenishmentTaskRepository.findAll()).thenReturn(List.of(task, task2));
        when(replenishmentTaskMapper.toResponse(task)).thenReturn(response);
        when(replenishmentTaskMapper.toResponse(task2)).thenReturn(response2);

        List<ReplenishmentTaskResponse> result = replenishmentTaskService.getAllReplenishmentTasks();

        assertThat(result).hasSize(2).containsExactly(response, response2);
    }

    @Test
    void getAllReplenishmentTasks_emptyRepository_returnsEmptyList() {
        when(replenishmentTaskRepository.findAll()).thenReturn(List.of());

        List<ReplenishmentTaskResponse> result = replenishmentTaskService.getAllReplenishmentTasks();

        assertThat(result).isEmpty();
    }

    @Test
    void searchReplenishmentTasks_withFilters_returnsMappedResults() {
        ReplenishmentTaskSearchRequest request = new ReplenishmentTaskSearchRequest(1L, null, null, ReplenishmentTaskStatus.CREATED, null, null);

        when(replenishmentTaskRepository.filter(1L, null, null, ReplenishmentTaskStatus.CREATED, null, null))
                .thenReturn(List.of(task));
        when(replenishmentTaskMapper.toResponse(task)).thenReturn(response);

        List<ReplenishmentTaskResponse> result = replenishmentTaskService.searchReplenishmentTasks(request);

        assertThat(result).hasSize(1).containsExactly(response);
    }

    @Test
    void searchReplenishmentTasks_noMatches_returnsEmptyList() {
        ReplenishmentTaskSearchRequest request = new ReplenishmentTaskSearchRequest(999L, null, null, null, null, null);

        when(replenishmentTaskRepository.filter(999L, null, null, null, null, null))
                .thenReturn(List.of());

        List<ReplenishmentTaskResponse> result = replenishmentTaskService.searchReplenishmentTasks(request);

        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentUsername_returnsUsernameFromSecurityContext() {
        mockSecurityContext("testUser");

        String username = replenishmentTaskService.getCurrentUsername();

        assertThat(username).isEqualTo("testUser");
    }

    @Test
    void validateReplenishmentTaskRequest_nullUsername_throwsInvalidRequestException() {
        assertThatThrownBy(() -> replenishmentTaskService.validateReplenishmentTaskRequest(null, ReplenishmentTaskStatus.CREATED))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Username is required");
    }

    @Test
    void validateReplenishmentTaskRequest_emptyUsername_throwsInvalidRequestException() {
        assertThatThrownBy(() -> replenishmentTaskService.validateReplenishmentTaskRequest("", ReplenishmentTaskStatus.CREATED))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Username is required");
    }

    @Test
    void validateReplenishmentTaskRequest_statusNotCreated_throwsInvalidRequestException() {
        assertThatThrownBy(() -> replenishmentTaskService.validateReplenishmentTaskRequest("john", ReplenishmentTaskStatus.ASSIGNED))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("status must be CREATED");
    }

    @Test
    void validateReplenishmentTaskRequest_validInput_doesNotThrow() {
        replenishmentTaskService.validateReplenishmentTaskRequest("john", ReplenishmentTaskStatus.CREATED);
    }
}