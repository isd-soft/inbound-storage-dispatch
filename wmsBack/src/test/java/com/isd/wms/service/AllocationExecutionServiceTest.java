package com.isd.wms.service;

import com.isd.wms.dto.allocation.*;
import com.isd.wms.entity.*;
import com.isd.wms.enums.*;
import com.isd.wms.exception.InvalidRequestException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationExecutionServiceTest {

    @Mock
    private AllocationRepository allocationRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private AllocationExecutionService allocationExecutionService;

    private User operator;
    private Allocation allocation;
    private Stock stock;
    private Task task;
    private OrderLine orderLine;
    private Order order;
    private Allocation nextAllocationSameLocation;

    @BeforeEach
    void setUp() {
        operator = user(1L, "operator");

        Product product = product(10L, "Coca-Cola", "SKU-001");
        Location location = location(20L, "PICK-01");

        stock = new Stock(30L, product, location, 50, 10, null, null, null);
        task = task(40L, TaskStatus.CREATED);
        allocation = allocation(50L, operator, task, stock, 10, Status.ASSIGNED);
        ReflectionTestUtils.setField(allocation, "createdAt", LocalDateTime.of(2026, 6, 15, 9, 0));
        order = order(60L, OrderStatus.IN_PROGRESS);
        orderLine = orderLine(70L, order, product, Status.IN_PROGRESS);
        ReflectionTestUtils.setField(order, "orderLines", List.of(orderLine));

        Task nextTask = new Task(null, TaskType.PICKING_ORDER, 4);
        ReflectionTestUtils.setField(nextTask, "id", 41L);
        nextTask.setOperator(operator);
        nextAllocationSameLocation = allocation(51L, operator, nextTask, stock, 4, Status.ASSIGNED);
        ReflectionTestUtils.setField(nextAllocationSameLocation, "createdAt", LocalDateTime.of(2026, 6, 15, 9, 1));

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("operator");
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userRepository.findByUsername("operator")).thenReturn(Optional.of(operator));
        lenient().when(securityFacade.getCurrentUsername()).thenReturn("operator");
        lenient().when(securityFacade.getCurrentUser()).thenReturn(operator);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAssignedAllocationsSuccessfully() {
        when(allocationRepository.findByOperatorAndStatuses(operator, List.of(Status.ASSIGNED, Status.IN_PROGRESS)))
            .thenReturn(List.of(allocation));

        List<AllocationExecutionResponse> responses = allocationExecutionService.getAssignedAllocations();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().allocationId()).isEqualTo(50L);
        assertThat(responses.getFirst().requiredQuantity()).isEqualTo(10);
    }

//    @Test
//    void startAssignedAllocationsSuccessfully() {
//        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
//        when(allocationRepository.save(allocation)).thenReturn(allocation);
//
//        AllocationExecutionResponse response = allocationExecutionService.startAllocation(50L);
//
//        assertThat(allocation.getStatus()).isEqualTo(Status.IN_PROGRESS);
//        assertThat(response.status()).isEqualTo("IN_PROGRESS");
//    }
//
//    @Test
//    void failWhenAllocationIsNotAssignedToCurrentOperator() {
//        ReflectionTestUtils.setField(allocation, "operator", otherOperator);
//        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
//
//        assertThatThrownBy(() -> allocationExecutionService.startAllocation(50L))
//                .isInstanceOf(InvalidRequestException.class)
//                .hasMessage("Allocation is not assigned to current operator");
//    }

    @Test
    void scanCorrectSourceLocation() {
        setStatus(Status.IN_PROGRESS);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);

        allocationExecutionService.scanSourceLocation(50L, new BarcodeScanRequest("PICK-01"));

        assertThat(allocation.isSourceLocationScanned()).isTrue();
    }

    @Test
    void failWhenSourceLocationBarcodeIsWrong() {
        setStatus(Status.IN_PROGRESS);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.scanSourceLocation(50L, new BarcodeScanRequest("WRONG")))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Wrong source location barcode");
    }

    @Test
    void scanCorrectProductSkuBarcode() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "sourceLocationScanned", true);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(stockRepository.findByProductIdAndLocationId(10L, 20L))
            .thenReturn(Optional.of(stock));
        when(allocationRepository.save(allocation)).thenReturn(allocation);

        allocationExecutionService.scanProduct(50L, new BarcodeScanRequest("SKU-001"));

        assertThat(allocation.isProductScanned()).isTrue();
    }

    @Test
    void failWhenProductBarcodeIsWrong() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "sourceLocationScanned", true);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.scanProduct(50L, new BarcodeScanRequest("WRONG")))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Wrong product barcode");
    }

    @Test
    void confirmPickedQuantitySuccessfully() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "productScanned", true);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);

        allocationExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(10));

        assertThat(allocation.getPickedQuantity()).isEqualTo(10);
    }

    @Test
    void failWhenPickedQuantityIsGreaterThanRequiredQuantity() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "productScanned", true);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(11)))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Picked quantity cannot exceed required quantity");
    }

    @Test
    void failWhenStockQuantityIsNotEnough() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "productScanned", true);
        ReflectionTestUtils.setField(stock, "quantity", 5);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(10)))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Not enough stock available");
    }

    @Test
    void completeAllocationSuccessfullyAndUpdatesParents() {
        prepareAllocationForCompletion();
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);

        AllocationCompletionResponse response = allocationExecutionService.completeAllocation(50L);

        assertThat(response.status()).isEqualTo(AllocationCompletionStatus.COMPLETED);
        assertThat(allocation.getStatus()).isEqualTo(Status.COMPLETED);
        verify(workflowService).executeAllocationCompletion(allocation);
    }

    @Test
    void completeAllocationDecreasesStockQuantityAndReservedQuantity() {
        prepareAllocationForCompletion();
        ReflectionTestUtils.setField(task, "taskType", TaskType.PICKING_ORDER);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);

        allocationExecutionService.completeAllocation(50L);

        assertThat(stock.getQuantity()).isEqualTo(40);
        assertThat(stock.getReservedQuantity()).isZero();
    }

    @Test
    void completeAllocationCreatesInventoryHistory() {
        prepareAllocationForCompletion();
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);

        allocationExecutionService.completeAllocation(50L);

        verify(inventoryService).recordPickingHistory(stock, 10, operator);
    }

    @Test
    void completeAllocationAutoStartsNextProductInSameLocationWithoutRescan() {
        ReflectionTestUtils.setField(task, "taskType", TaskType.PICKING_ORDER);
        prepareAllocationForCompletion();
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(allocationRepository.save(allocation)).thenReturn(allocation);
        when(orderLineRepository.findByTaskId(40L)).thenReturn(Optional.of(orderLine));
        when(allocationRepository.findAllByOrder(order)).thenReturn(List.of(allocation, nextAllocationSameLocation));

        allocationExecutionService.completeAllocation(50L);

        assertThat(nextAllocationSameLocation.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(nextAllocationSameLocation.isSourceLocationScanned()).isTrue();
        verify(allocationRepository).save(nextAllocationSameLocation);
    }

    @Test
    void startAllocationMovesAssignedPickingOrderToInProgress() {
        ReflectionTestUtils.setField(task, "taskType", TaskType.PICKING_ORDER);
        ReflectionTestUtils.setField(orderLine, "status", Status.ASSIGNED);
        ReflectionTestUtils.setField(order, "status", OrderStatus.ASSIGNED);

        when(allocationRepository.findOldestAssignedAllocationId("operator")).thenReturn(Optional.of(50L));
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));
        when(orderLineRepository.findByTaskId(40L)).thenReturn(Optional.of(orderLine));

        Long allocationId = allocationExecutionService.startAllocation();

        assertThat(allocationId).isEqualTo(50L);
        assertThat(orderLine.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        verify(orderRepository).save(order);
    }

    @Test
    void failWhenPickingQuantityDoesNotMatchRequiredForPickingTask() {
        ReflectionTestUtils.setField(task, "taskType", TaskType.PICKING_ORDER);
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "productScanned", true);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.confirmPickedQuantity(50L, new ConfirmPickedQuantityRequest(9)))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Picked quantity must match required quantity for picking tasks");
    }

    @Test
    void failWhenTryingToCompleteAllocationWithoutScans() {
        setStatus(Status.IN_PROGRESS);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.completeAllocation(50L))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Source location must be scanned first");
        verify(stockRepository, never()).save(any());
    }

    @Test
    void failWhenAllocationIsAlreadyCompleted() {
        setStatus(Status.COMPLETED);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.completeAllocation(50L))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Allocation is already completed");
    }

    @Test
    void failWhenAllocationIsCancelled() {
        setStatus(Status.CANCELED);
        when(allocationRepository.findById(50L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> allocationExecutionService.completeAllocation(50L))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("Allocation is cancelled");
    }

    private void prepareAllocationForCompletion() {
        setStatus(Status.IN_PROGRESS);
        ReflectionTestUtils.setField(allocation, "sourceLocationScanned", true);
        ReflectionTestUtils.setField(allocation, "productScanned", true);
        ReflectionTestUtils.setField(allocation, "pickedQuantity", 10);
        doAnswer(invocation -> {
            stock.removeQuantity(allocation.getPickedQuantity().orElse(0));
            return new AllocationCompletionResult(AllocationCompletionStatus.COMPLETED, task.getTaskType(), allocation.getId());
        }).when(workflowService).executeAllocationCompletion(allocation);
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
        ReflectionTestUtils.setField(task, "status", status);
        return task;
    }

    private Allocation allocation(Long id, User operator, Task task, Stock stock, Integer quantity, Status status) {
        Allocation allocation = new Allocation(task, stock, quantity, status);
        ReflectionTestUtils.setField(allocation, "id", id);
        task.setOperator(operator);
        return allocation;
    }

    private Order order(Long id, OrderStatus status) {
        Order order = new Order("ORDER-" + id);
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private OrderLine orderLine(Long id, Order order, Product product, Status status) {
        OrderLine orderLine = new OrderLine(order, product, 10);
        ReflectionTestUtils.setField(orderLine, "id", id);
        ReflectionTestUtils.setField(orderLine, "status", status);
        return orderLine;
    }

    private void setStatus(Status status) {
        ReflectionTestUtils.setField(allocation, "status", status);
    }
}
