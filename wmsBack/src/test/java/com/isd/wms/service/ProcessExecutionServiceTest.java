package com.isd.wms.service;

import com.isd.wms.dto.process.BarcodeScanRequest;
import com.isd.wms.dto.process.ConfirmPickedQuantityRequest;
import com.isd.wms.dto.process.ProcessExecutionResponse;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Order;
import com.isd.wms.entity.OrderLine;
import com.isd.wms.entity.Process;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.Task;
import com.isd.wms.entity.User;
import com.isd.wms.enums.*;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.repository.OrderLineRepository;
import com.isd.wms.repository.OrderRepository;
import com.isd.wms.repository.ProcessRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.TaskRepository;
import com.isd.wms.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessExecutionServiceTest {

    @Mock
    private ProcessRepository processRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ProcessExecutionService processExecutionService;

    private User operator;
    private User otherOperator;
    private Process process;
    private Stock stock;
    private Task task;
    private OrderLine orderLine;
    private Order order;

    @BeforeEach
    void setUp() {
        operator = user(1L, "operator");
        otherOperator = user(2L, "other");

        Product product = product(10L, "Coca-Cola", "SKU-001");
        Location location = location(20L, "PICK-01");

        stock = new Stock(30L, product, location, 50, 10, null, null, null);
        task = task(40L, TaskStatus.CREATED);
        process = process(50L, operator, task, stock, 10, Status.ASSIGNED);
        order = order(60L, Status.IN_PROGRESS);
        orderLine = orderLine(70L, order, task, product, Status.IN_PROGRESS);
        ReflectionTestUtils.setField(order, "orderLines", List.of(orderLine));

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("operator");
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAssignedProcessesSuccessfully() {
        when(processRepository.findByOperatorAndStatuses(operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
                .thenReturn(List.of(process));

        List<ProcessExecutionResponse> responses = processExecutionService.getAssignedProcesses();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).processId()).isEqualTo(50L);
        assertThat(responses.get(0).requiredQuantity()).isEqualTo(10);
    }

//    @Test
//    void startAssignedProcessSuccessfully() {
//        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
//        when(processRepository.save(process)).thenReturn(process);
//
//        ProcessExecutionResponse response = processExecutionService.startProcess(50L);
//
//        assertThat(process.getStatus()).isEqualTo(Status.IN_PROGRESS);
//        assertThat(response.status()).isEqualTo("IN_PROGRESS");
//    }
//
//    @Test
//    void failWhenProcessIsNotAssignedToCurrentOperator() {
//        ReflectionTestUtils.setField(process, "operator", otherOperator);
//        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
//
//        assertThatThrownBy(() -> processExecutionService.startProcess(50L))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessage("Process is not assigned to current operator");
//    }

    @Test
    void scanCorrectSourceLocation() {
        setStatus(Status.IN_PROGRESS);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);

        processExecutionService.scanSourceLocation(50L, new BarcodeScanRequest("PICK-01"));

        assertThat(process.isSourceLocationScanned()).isTrue();
    }

    @Test
    void failWhenSourceLocationBarcodeIsWrong() {
        setStatus(Status.IN_PROGRESS);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.scanSourceLocation(50L, new BarcodeScanRequest("WRONG")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Wrong source location barcode");
    }

    @Test
    void scanCorrectProductSkuBarcode() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(process, "sourceLocationScanned", true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(stockRepository.findByProductIdAndLocationId(10L, 20L))
                .thenReturn(Optional.of(stock));
        when(processRepository.save(process)).thenReturn(process);

        processExecutionService.scanProduct(50L, new BarcodeScanRequest("SKU-001"));

        assertThat(process.isProductScanned()).isTrue();
    }

    @Test
    void failWhenProductBarcodeIsWrong() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(process, "sourceLocationScanned", true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.scanProduct(50L, new BarcodeScanRequest("WRONG")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Wrong product/SKU barcode");
    }

    @Test
    void confirmPickedQuantitySuccessfully() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(process, "productScanned", true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);

        processExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(10));

        assertThat(process.getPickedQuantity()).isEqualTo(10);
    }

    @Test
    void failWhenPickedQuantityIsGreaterThanRequiredQuantity() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(process, "productScanned", true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(11)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Picked quantity cannot exceed required quantity");
    }

    @Test
    void failWhenStockQuantityIsNotEnough() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(process, "productScanned", true);
        ReflectionTestUtils.setField(stock, "quantity", 5);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(10)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Not enough stock available");
    }

    @Test
    void completeProcessSuccessfullyAndUpdatesParents() {
        prepareProcessForCompletion();
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);
        when(processRepository.findAllByTaskId(40L)).thenReturn(List.of(process));
        when(orderLineRepository.findByTaskId(40L)).thenReturn(Optional.of(orderLine));

        ProcessExecutionResponse response = processExecutionService.completeProcess(50L);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(process.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(orderLine.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void completeProcessDecreasesStockQuantityAndReservedQuantity() {
        prepareProcessForCompletion();
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);
        when(processRepository.findAllByTaskId(40L)).thenReturn(List.of(process));
        when(orderLineRepository.findByTaskId(40L)).thenReturn(Optional.empty());

        processExecutionService.completeProcess(50L);

        ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(40);
        assertThat(stockCaptor.getValue().getReservedQuantity()).isZero();
    }

    @Test
    void completeProcessCreatesInventoryHistory() {
        prepareProcessForCompletion();
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);
        when(processRepository.findAllByTaskId(40L)).thenReturn(List.of(process));
        when(orderLineRepository.findByTaskId(40L)).thenReturn(Optional.empty());

        processExecutionService.completeProcess(50L);

        verify(inventoryService).recordPickingHistory(stock, 10, operator);
    }

    @Test
    void failWhenTryingToCompleteProcessWithoutScans() {
        setStatus(Status.IN_PROGRESS);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.completeProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Source location must be scanned first");
        verify(stockRepository, never()).save(any());
    }

    @Test
    void failWhenProcessIsAlreadyCompleted() {
        setStatus(Status.COMPLETED);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.completeProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Process is already completed");
    }

    @Test
    void failWhenProcessIsCancelled() {
        setStatus(Status.CANCELED);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.completeProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Process is cancelled");
    }

    private void prepareProcessForCompletion() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(process, "sourceLocationScanned", true);
        ReflectionTestUtils.setField(process, "productScanned", true);
        ReflectionTestUtils.setField(process, "pickedQuantity", 10);
    }

    private User user(Long id, String username) {
        User user = new User(username, username + "@example.com", "password", Role.ROLE_OPERATOR, false, null, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Product product(Long id, String name, String barcode) {
        Product product = new Product(name, barcode, null, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private Location location(Long id, String barcode) {
        Location location = new Location("name", barcode, Zone.PICKING, null, true);
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }

    private Task task(Long id, TaskStatus status) {
        Task task = new Task(null, TaskType.REPLENISHMENT, 0);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private Process process(Long id, User operator, Task task, Stock stock, Integer quantity, Status status) {
        Process process = new Process(task, stock, quantity, status);
        ReflectionTestUtils.setField(process, "id", id);
        ReflectionTestUtils.setField(process, "operator", operator);
        return process;
    }

    private Order order(Long id, Status status) {
        Order order = new Order("ORDER-" + id);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private OrderLine orderLine(Long id, Order order, Task task, Product product, Status status) {
        OrderLine orderLine = new OrderLine(order, task, product, 10);
        ReflectionTestUtils.setField(orderLine, "id", id);
        ReflectionTestUtils.setField(orderLine, "status", status);
        return orderLine;
    }

    private void setStatus(Status status) {
        ReflectionTestUtils.setField(process, "status", status);
    }
}
