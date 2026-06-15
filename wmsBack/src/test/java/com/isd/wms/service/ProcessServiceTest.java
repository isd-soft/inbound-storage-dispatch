//package com.isd.wms.service;
//
//import com.isd.wms.dto.allocation.ProcessOperatorResponse;
//import com.isd.wms.entity.Location;
//import com.isd.wms.entity.Process;
//import com.isd.wms.entity.Product;
//import com.isd.wms.entity.Stock;
//import com.isd.wms.entity.Task;
//import com.isd.wms.entity.User;
//import com.isd.wms.enums.Status;
//import com.isd.wms.exception.InvalidRequestException;
//import com.isd.wms.repository.AllocationRepository  ;
//import com.isd.wms.repository.UserRepository;
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
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class allocationServiceTest {
//
//    @Mock
//    private AllocationRepository  allocationRepository ;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private WorkflowService workflowService;
//
//    @InjectMocks
//    private AllocationService allocationService;
//
//    private User operator;
//    private Allocation allocation;
//
//    @BeforeEach
//    void setUp() {
//        operator = new User();
//        operator.setId(1L);
//        operator.setUsername("testOperator");
//
//        Product product = new Product();
//        product.setId(10L);
//        product.setName("Test Product");
//
//        Location location = new Location();
//        location.setBarcode("ZONE-A");
//
//        Stock stock = new Stock();
//        stock.setProduct(product);
//        stock.setLocation(location);
//
//        Task task = new Task();
//        task.setId(100L);
//
//        process = new Process();
//        allocation.setId(50L);
//        allocation.setQuantity(10);
//        allocation.setStatus(Status.CREATED);
//        allocation.setStock(stock);
//        allocation.setTask(task);
//
//        Authentication authentication = mock(Authentication.class);
//        SecurityContext securityContext = mock(SecurityContext.class);
//
//        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
//        SecurityContextHolder.setContext(securityContext);
//
//        lenient().when(authentication.getName()).thenReturn("testOperator");
//        lenient().when(userRepository.findByUsername("testOperator")).thenReturn(Optional.of(operator));
//    }
//
//    @Test
//    void getAvailableProcesses_ShouldReturnList() {
//        when(allocationRepository .findByStatus(Status.CREATED)).thenReturn(List.of(process));
//
//        List<ProcessOperatorResponse> result = allocationService.getAvailableProcesses();
//
//        assertFalse(result.isEmpty());
//        assertEquals(50L, result.get(0).id());
//        assertEquals("Test Product", result.get(0).productName());
//        verify(allocationRepository ).findByStatus(Status.CREATED);
//    }
//
//    @Test
//    void assignProcess_WhenStatusCreated_ShouldAssignToOperator() {
//        when(allocationRepository .findById(50L)).thenReturn(Optional.of(process));
//        when(allocationRepository .save(any(allocation.class))).thenReturn(process);
//
//        ProcessOperatorResponse result = allocationService.assignProcess(50L);
//
//        assertEquals(Status.ASSIGNED, allocation.getStatus());
//        assertEquals(operator, allocation.getOperator().orElseThrow());
//        assertEquals(Status.ASSIGNED, result.status());
//        verify(allocationRepository ).save(process);
//    }
//
//    @Test
//    void assignProcess_WhenStatusAlreadyAssigned_ShouldThrowException() {
//        allocation.setStatus(Status.ASSIGNED);
//        when(allocationRepository .findById(50L)).thenReturn(Optional.of(process));
//
//        assertThrows(InvalidRequestException.class, () -> allocationService.assignProcess(50L));
//        verify(allocationRepository , never()).save(any());
//    }
//
//    @Test
//    void completeProcess_WhenAssignedToCurrentUser_ShouldCompleteAndTriggerWorkflow() {
//        allocation.setStatus(Status.ASSIGNED);
//        allocation.setOperator(operator);
//
//        when(allocationRepository .findById(50L)).thenReturn(Optional.of(process));
//        when(allocationRepository .save(any(allocation.class))).thenReturn(process);
//
//        ProcessOperatorResponse result = allocationService.completeProcess(50L);
//
//        assertEquals(Status.COMPLETED, allocation.getStatus());
//        assertEquals(Status.COMPLETED, result.status());
//
//        verify(allocationRepository ).save(process);
//        verify(workflowService).executeProcessCompletion(process);
//    }
//
//    @Test
//    void completeProcess_WhenAssignedToDifferentUser_ShouldThrowException() {
//        User anotherOperator = new User();
//        anotherOperator.setId(2L);
//        allocation.setStatus(Status.ASSIGNED);
//        allocation.setOperator(anotherOperator);
//
//        when(allocationRepository .findById(50L)).thenReturn(Optional.of(process));
//
//        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> allocationService.completeProcess(50L));
//        assertEquals("You can only complete your own processes", exception.getMessage());
//        verify(workflowService, never()).executeProcessCompletion(any());
//    }
//}
