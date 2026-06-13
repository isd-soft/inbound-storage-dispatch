//package com.isd.wms.service;
//
//import com.isd.wms.dto.process.ProcessOperatorResponse;
//import com.isd.wms.entity.Location;
//import com.isd.wms.entity.Process;
//import com.isd.wms.entity.Product;
//import com.isd.wms.entity.Stock;
//import com.isd.wms.entity.Task;
//import com.isd.wms.entity.User;
//import com.isd.wms.enums.Status;
//import com.isd.wms.exception.InvalidRequestException;
//import com.isd.wms.repository.ProcessRepository;
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
//class ProcessServiceTest {
//
//    @Mock
//    private ProcessRepository processRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private WorkflowService workflowService;
//
//    @InjectMocks
//    private ProcessService processService;
//
//    private User operator;
//    private Process process;
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
//        process.setId(50L);
//        process.setQuantity(10);
//        process.setStatus(Status.CREATED);
//        process.setStock(stock);
//        process.setTask(task);
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
//        when(processRepository.findByStatus(Status.CREATED)).thenReturn(List.of(process));
//
//        List<ProcessOperatorResponse> result = processService.getAvailableProcesses();
//
//        assertFalse(result.isEmpty());
//        assertEquals(50L, result.get(0).id());
//        assertEquals("Test Product", result.get(0).productName());
//        verify(processRepository).findByStatus(Status.CREATED);
//    }
//
//    @Test
//    void assignProcess_WhenStatusCreated_ShouldAssignToOperator() {
//        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
//        when(processRepository.save(any(Process.class))).thenReturn(process);
//
//        ProcessOperatorResponse result = processService.assignProcess(50L);
//
//        assertEquals(Status.ASSIGNED, process.getStatus());
//        assertEquals(operator, process.getOperator().orElseThrow());
//        assertEquals(Status.ASSIGNED, result.status());
//        verify(processRepository).save(process);
//    }
//
//    @Test
//    void assignProcess_WhenStatusAlreadyAssigned_ShouldThrowException() {
//        process.setStatus(Status.ASSIGNED);
//        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
//
//        assertThrows(InvalidRequestException.class, () -> processService.assignProcess(50L));
//        verify(processRepository, never()).save(any());
//    }
//
//    @Test
//    void completeProcess_WhenAssignedToCurrentUser_ShouldCompleteAndTriggerWorkflow() {
//        process.setStatus(Status.ASSIGNED);
//        process.setOperator(operator);
//
//        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
//        when(processRepository.save(any(Process.class))).thenReturn(process);
//
//        ProcessOperatorResponse result = processService.completeProcess(50L);
//
//        assertEquals(Status.COMPLETED, process.getStatus());
//        assertEquals(Status.COMPLETED, result.status());
//
//        verify(processRepository).save(process);
//        verify(workflowService).executeProcessCompletion(process);
//    }
//
//    @Test
//    void completeProcess_WhenAssignedToDifferentUser_ShouldThrowException() {
//        User anotherOperator = new User();
//        anotherOperator.setId(2L);
//        process.setStatus(Status.ASSIGNED);
//        process.setOperator(anotherOperator);
//
//        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
//
//        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> processService.completeProcess(50L));
//        assertEquals("You can only complete your own processes", exception.getMessage());
//        verify(workflowService, never()).executeProcessCompletion(any());
//    }
//}
