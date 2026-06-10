package com.isd.wms.service;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.dto.inventory.AdjustStockRequest;
import com.isd.wms.dto.inventory.InventoryHistoryResponse;
import com.isd.wms.dto.inventory.RemoveStockRequest;
import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.entity.Category;
import com.isd.wms.entity.InventoryHistory;
import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Role;
import com.isd.wms.exception.InsufficientStockException;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.LocationNotFoundException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.exception.StockNotFoundException;
import com.isd.wms.exception.UserNotFoundException;
import com.isd.wms.mapper.InventoryHistoryMapper;
import com.isd.wms.mapper.StockMapper;
import com.isd.wms.repository.InventoryHistoryRepository;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository UserRepository;

    private InventoryService inventoryService;
    private Validator validator;
    private Product product;
    private Location location;
    private User user;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(
                stockRepository,
                inventoryHistoryRepository,
                productRepository,
                locationRepository,
                UserRepository,
                new StockMapper(),
                new InventoryHistoryMapper()
        );
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        product = product(1L, "Milk");
        location = location(2L, "A-01");
        user = user(3L, "supervisor");
    }

    @Test
    void addsNewStockAndCreatesHistory() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(location));
        when(UserRepository.findById(3L)).thenReturn(Optional.of(user));
        when(stockRepository.findByProductIdAndLocationId(1L, 2L))
                .thenReturn(Optional.empty());
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> {
            Stock savedStock = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedStock, "id", 10L);
            return savedStock;
        });

        StockResponse response = inventoryService.addStock(new AddStockRequest(
                1L, 2L, 5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 3L
        ));

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getQuantity()).isEqualTo(5);
        assertThat(response.getSku()).isEqualTo("SKU-1");

        ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());
        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getOperationType()).isEqualTo(InventoryOperationType.ADD_STOCK);
        assertThat(history.getAlteredQuantity()).isEqualTo(5);
        assertThat(history.getQuantityAfterChange()).isEqualTo(5);
        assertThat(history.getDestinationLocation()).isEqualTo(location);
        assertThat(history.getUser()).isEqualTo(user);
    }

    @Test
    void addsStockToExistingRecord() {
        Stock stock = stock(10L, product, location, "SKU-1", 7);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(location));
        when(UserRepository.findById(3L)).thenReturn(Optional.of(user));
        when(stockRepository.findByProductIdAndLocationId(1L, 2L))
                .thenReturn(Optional.of(stock));
        when(stockRepository.save(stock)).thenReturn(stock);

        StockResponse response = inventoryService.addStock(new AddStockRequest(
                1L, 2L, 3, null, null, 3L
        ));

        assertThat(response.getQuantity()).isEqualTo(10);
        verify(inventoryHistoryRepository).save(any(InventoryHistory.class));
    }

    @Test
    void rejectsAddStockWithInvalidQuantity() {
        assertThat(validator.validate(new AddStockRequest(
                1L, 2L, 0, null, null, 3L
        ))).isNotEmpty();
    }

    @Test
    void rejectsAddStockWithMissingProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.addStock(new AddStockRequest(
                99L, 2L, 5, null, null, 3L
        ))).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void rejectsAddStockWithMissingLocation() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.addStock(new AddStockRequest(
                1L, 99L, 5, null, null, 3L
        ))).isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void removesStockAndCreatesHistory() {
        Stock stock = stock(10L, product, location, "SKU-1", 8);
        when(stockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(UserRepository.findById(3L)).thenReturn(Optional.of(user));
        when(stockRepository.save(stock)).thenReturn(stock);

        StockResponse response = inventoryService.removeStock(new RemoveStockRequest(10L, 3, 3L));

        assertThat(response.getQuantity()).isEqualTo(5);

        ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());
        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getOperationType()).isEqualTo(InventoryOperationType.REMOVE_STOCK);
        assertThat(history.getAlteredQuantity()).isEqualTo(-3);
        assertThat(history.getQuantityAfterChange()).isEqualTo(5);
        assertThat(history.getSourceLocation()).isEqualTo(location);
    }

    @Test
    void rejectsRemovingMoreThanAvailableStock() {
        Stock stock = stock(10L, product, location, "SKU-1", 2);
        when(stockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(UserRepository.findById(3L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> inventoryService.removeStock(new RemoveStockRequest(10L, 3, 3L)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void adjustsStockAndCreatesHistoryWithDifference() {
        Stock stock = stock(10L, product, location, "SKU-1", 8);
        when(stockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(UserRepository.findById(3L)).thenReturn(Optional.of(user));
        when(stockRepository.save(stock)).thenReturn(stock);

        StockResponse response = inventoryService.adjustStock(new AdjustStockRequest(10L, 12, 3L));

        assertThat(response.getQuantity()).isEqualTo(12);

        ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());
        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getOperationType()).isEqualTo(InventoryOperationType.ADJUST_STOCK);
        assertThat(history.getAlteredQuantity()).isEqualTo(4);
        assertThat(history.getQuantityAfterChange()).isEqualTo(12);
    }

    @Test
    void rejectsNegativeAdjustQuantity() {
        assertThat(validator.validate(new AdjustStockRequest(10L, -1, 3L)))
                .isNotEmpty();
    }

    @Test
    void rejectsOperationWithMissingUser() {
        Stock stock = stock(10L, product, location, "SKU-1", 8);
        when(stockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(UserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.removeStock(new RemoveStockRequest(10L, 1, 99L)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void rejectsUnknownStock() {
        when(stockRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getStockById(99L))
                .isInstanceOf(StockNotFoundException.class);
    }

    @Test
    void returnsHistoryForStockByProductSkuAndLocation() {
        Stock stock = stock(10L, product, location, "SKU-1", 8);

        InventoryHistory history = new InventoryHistory();
        history.setProduct(product);
        history.setSku("SKU-1");
        history.setAlteredQuantity(8);
        history.setQuantityAfterChange(8);
        history.setDestinationLocation(location);
        history.setOperationType(InventoryOperationType.ADD_STOCK);
        history.setUser(user);
        ReflectionTestUtils.setField(history, "id", 20L);

        when(stockRepository.findById(10L)).thenReturn(Optional.of(stock));
        when(inventoryHistoryRepository
                .findByProductIdAndSourceLocationIdOrProductIdAndDestinationLocationId(
                        1L, 2L, 1L, 2L))
                .thenReturn(List.of(history));

        List<InventoryHistoryResponse> responses = inventoryService.getHistoryForStock(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(20L);
        assertThat(responses.getFirst().getOperationType()).isEqualTo("ADD_STOCK");
    }

    private Category category(Long id, String name) {
        Category result = new Category(name);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private Product product(Long id, String name) {
        Product result = new Product(name, "SKU-1", null, category(100L, "Dairy"));
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private Location location(Long id, String locationCode) {
        Location result = new Location();
        ReflectionTestUtils.setField(result, "id", id);
        result.setLocationCode(locationCode);
        result.setAvailable(true);
        return result;
    }

    private User user(Long id, String username) {
        User result = new User();
        result.setUsername(username);
        result.setEmail(username + "@example.com");
        result.setPassword("password");
        result.setUserRole(Role.ROLE_SUPERVISOR);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private Stock stock(Long id, Product product, Location location, String sku, Integer quantity) {
        Stock result = new Stock();
        result.setProduct(product);
        result.setLocation(location);
        result.setQuantity(quantity);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
