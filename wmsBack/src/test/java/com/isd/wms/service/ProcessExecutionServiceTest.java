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
import com.isd.wms.enums.OrderStatus;
import com.isd.wms.enums.ProcessStatus;
import com.isd.wms.enums.TaskStatus;
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
        operator = new User();
        operator.setId(1L);
        operator.setUsername("operator");

        otherOperator = new User();
        otherOperator.setId(2L);
        otherOperator.setUsername("other");

        Product product = new Product();
        product.setId(10L);
        product.setName("Coca-Cola");

        Location location = new Location();
        location.setId(20L);
        location.setLocationCode("PICK-01");

        stock = Stock.builder()
                .id(30L)
                .product(product)
                .location(location)
                .sku("SKU-001")
                .quantity(50)
                .reservedQuantity(10)
                .build();

        task = new Task();
        task.setId(40L);
        task.setStatus(TaskStatus.CREATED);

        process = Process.builder()
                .id(50L)
                .operator(operator)
                .task(task)
                .stock(stock)
                .quantity(10)
                .status(ProcessStatus.ASSIGNED)
                .build();

        order = new Order();
        order.setId(60L);
        order.setStatus(OrderStatus.IN_PROCESS);

        orderLine = new OrderLine();
        orderLine.setId(70L);
        orderLine.setTask(task);
        orderLine.setOrder(order);
        orderLine.setStatus(OrderStatus.IN_PROCESS);
        order.setOrderLines(List.of(orderLine));

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
        when(processRepository.findByOperatorAndStatuses(operator, List.of(ProcessStatus.ASSIGNED, ProcessStatus.IN_PROGRESS)))
                .thenReturn(List.of(process));

        List<ProcessExecutionResponse> responses = processExecutionService.getAssignedProcesses();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).processId()).isEqualTo(50L);
        assertThat(responses.get(0).requiredQuantity()).isEqualTo(10);
    }

    @Test
    void startAssignedProcessSuccessfully() {
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);

        ProcessExecutionResponse response = processExecutionService.startProcess(50L);

        assertThat(process.getStatus()).isEqualTo(ProcessStatus.IN_PROGRESS);
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void failWhenProcessIsNotAssignedToCurrentOperator() {
        process.setOperator(otherOperator);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.startProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Process is not assigned to current operator");
    }

    @Test
    void scanCorrectSourceLocation() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);

        processExecutionService.scanSourceLocation(50L, new BarcodeScanRequest("PICK-01"));

        assertThat(process.isSourceLocationScanned()).isTrue();
    }

    @Test
    void failWhenSourceLocationBarcodeIsWrong() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.scanSourceLocation(50L, new BarcodeScanRequest("WRONG")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Wrong source location barcode");
    }

    @Test
    void scanCorrectProductSkuBarcode() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        process.setSourceLocationScanned(true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(stockRepository.findByProductIdAndSkuIgnoreCaseAndLocationId(10L, "SKU-001", 20L))
                .thenReturn(Optional.of(stock));
        when(processRepository.save(process)).thenReturn(process);

        processExecutionService.scanProduct(50L, new BarcodeScanRequest("SKU-001"));

        assertThat(process.isProductScanned()).isTrue();
    }

    @Test
    void failWhenProductBarcodeIsWrong() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        process.setSourceLocationScanned(true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.scanProduct(50L, new BarcodeScanRequest("WRONG")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Wrong product/SKU barcode");
    }

    @Test
    void confirmPickedQuantitySuccessfully() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        process.setProductScanned(true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));
        when(processRepository.save(process)).thenReturn(process);

        processExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(10));

        assertThat(process.getPickedQuantity()).isEqualTo(10);
    }

    @Test
    void failWhenPickedQuantityIsGreaterThanRequiredQuantity() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        process.setProductScanned(true);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(11)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Picked quantity cannot exceed required quantity");
    }

    @Test
    void failWhenStockQuantityIsNotEnough() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        process.setProductScanned(true);
        stock.setQuantity(5);
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
        assertThat(process.getStatus()).isEqualTo(ProcessStatus.COMPLETED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(orderLine.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
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
        process.setStatus(ProcessStatus.IN_PROGRESS);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.completeProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Source location must be scanned first");
        verify(stockRepository, never()).save(any());
    }

    @Test
    void failWhenProcessIsAlreadyCompleted() {
        process.setStatus(ProcessStatus.COMPLETED);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.completeProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Process is already completed");
    }

    @Test
    void failWhenProcessIsCancelled() {
        process.setStatus(ProcessStatus.CANCELED);
        when(processRepository.findById(50L)).thenReturn(Optional.of(process));

        assertThatThrownBy(() -> processExecutionService.completeProcess(50L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Process is cancelled");
    }

    private void prepareProcessForCompletion() {
        process.setStatus(ProcessStatus.IN_PROGRESS);
        process.setSourceLocationScanned(true);
        process.setProductScanned(true);
        process.setPickedQuantity(10);
    }
}
