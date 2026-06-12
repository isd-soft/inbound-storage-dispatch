package com.isd.wms.service;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.dto.inventory.AdjustStockRequest;
import com.isd.wms.dto.inventory.InventoryHistoryResponse;
import com.isd.wms.dto.inventory.RemoveStockRequest;
import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.entity.InventoryHistory;
import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final StockRepository stockRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final UserRepository UserRepository;
    private final StockMapper stockMapper;
    private final InventoryHistoryMapper inventoryHistoryMapper;

    public List<StockResponse> getAllStock() {
        return stockRepository.findAll().stream()
                .map(stockMapper::toResponse)
                .toList();
    }

    public StockResponse getStockById(Long stockId) {
        return stockMapper.toResponse(getStock(stockId));
    }

    @Transactional
    public StockResponse addStock(AddStockRequest request) {
        log.info("Adding stock: productId={}, locationId={}, quantity={}, userId={}",
                request.getProductId(), request.getLocationId(), request.getQuantity(), request.getUserId());

        Product product = getProduct(request.getProductId());
        Location location = getLocation(request.getLocationId());
        User user = getUser(request.getUserId());

        Stock stock = stockRepository.findByProductIdAndLocationId(product.getId(), location.getId())
                .orElseGet(() -> new Stock(product, location));

        stock.setQuantity(stock.getQuantity() + request.getQuantity());
        stock.setManufactureDate(request.getManufactureDate());
        stock.setExpirationDate(request.getExpirationDate());

        Stock savedStock = stockRepository.save(stock);

        createHistory(savedStock, request.getQuantity(), savedStock.getQuantity(), null, location,
                InventoryOperationType.ADD_STOCK, user);
        log.info("Stock added successfully: stockId={}, finalQuantity={}", savedStock.getId(), savedStock.getQuantity());
        return stockMapper.toResponse(savedStock);
    }

    @Transactional
    public StockResponse removeStock(RemoveStockRequest request) {
        log.info("Removing stock: stockId={}, quantity={}, userId={}",
                request.getStockId(), request.getQuantity(), request.getUserId());

        Stock stock = getStock(request.getStockId());
        User user = getUser(request.getUserId());

        int availableQuantity = stock.getQuantity() - stock.getReservedQuantity();

        if (request.getQuantity() > availableQuantity) {
            log.warn("Insufficient unreserved stock: stockId={}, requestedQuantity={}, availableQuantity={}, reservedQuantity={}, userId={}",
                    stock.getId(), request.getQuantity(), availableQuantity, stock.getReservedQuantity(), request.getUserId());
            throw new InsufficientStockException(stock.getId(), request.getQuantity(), availableQuantity);
        }

        int finalQuantity = stock.getQuantity() - request.getQuantity();
        stock.setQuantity(finalQuantity);
        Stock savedStock = stockRepository.save(stock);

        createHistory(savedStock, -request.getQuantity(), finalQuantity, savedStock.getLocation(), null,
                InventoryOperationType.REMOVE_STOCK, user);
        log.info("Stock removed successfully: stockId={}, finalQuantity={}", savedStock.getId(), finalQuantity);
        return stockMapper.toResponse(savedStock);
    }

    @Transactional
    public StockResponse adjustStock(AdjustStockRequest request) {
        log.info("Adjusting stock: stockId={}, newQuantity={}, userId={}",
                request.getStockId(), request.getNewQuantity(), request.getUserId());

        Stock stock = getStock(request.getStockId());

        if (request.getNewQuantity() < stock.getReservedQuantity()) {
            log.warn("Cannot adjust stock below reserved quantity: stockId={}, newQuantity={}, reservedQuantity={}",
                    stock.getId(), request.getNewQuantity(), stock.getReservedQuantity());
            throw new InvalidRequestException("Cannot adjust quantity below currently reserved quantity (" + stock.getReservedQuantity() + ")");
        }

        User user = getUser(request.getUserId());
        int oldQuantity = stock.getQuantity();
        int alteredQuantity = request.getNewQuantity() - oldQuantity;
        stock.setQuantity(request.getNewQuantity());
        Stock savedStock = stockRepository.save(stock);

        createHistory(savedStock, alteredQuantity, request.getNewQuantity(), savedStock.getLocation(), savedStock.getLocation(),
                InventoryOperationType.ADJUST_STOCK, user);
        log.info("Stock adjusted successfully: stockId={}, oldQuantity={}, newQuantity={}, alteredQuantity={}",
                savedStock.getId(), oldQuantity, request.getNewQuantity(), alteredQuantity);
        return stockMapper.toResponse(savedStock);
    }

    public List<InventoryHistoryResponse> getAllHistory() {
        return inventoryHistoryRepository.findAll().stream()
                .map(inventoryHistoryMapper::toResponse)
                .toList();
    }

    public List<InventoryHistoryResponse> getHistoryForStock(Long stockId) {
        Stock stock = getStock(stockId);
        Long productId = stock.getProduct().map(Product::getId).orElse(null);
        Long locationId = stock.getLocation() == null ? null : stock.getLocation().getId();
        return inventoryHistoryRepository
                .findByProductIdAndSourceLocationIdOrProductIdAndDestinationLocationId(
                        productId, locationId, productId, locationId)
                .stream()
                .map(inventoryHistoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public void recordPickingHistory(Stock stock, Integer pickedQuantity, User user) {
        createHistory(stock, -pickedQuantity, stock.getQuantity(), stock.getLocation(), null,
                InventoryOperationType.PICKING, user);
    }

    private void createHistory(
            Stock stock,
            Integer alteredQuantity,
            Integer quantityAfterChange,
            Location sourceLocation,
            Location destinationLocation,
            InventoryOperationType operationType,
            User user
    ) {
        Product product = stock.getProduct().orElse(null);
        InventoryHistory history = new InventoryHistory(
                product,
                product == null ? null : product.getBarcode(),
                alteredQuantity,
                quantityAfterChange,
                sourceLocation,
                destinationLocation,
                operationType,
                user
        );
        history.setTimestamp(LocalDateTime.now());
        inventoryHistoryRepository.save(history);
    }

    private Stock getStock(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> {
                    log.warn("Stock not found: stockId={}", stockId);
                    return new StockNotFoundException(stockId);
                });
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: productId={}", productId);
                    return new ProductNotFoundException(productId);
                });
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> {
                    log.warn("Location not found: locationId={}", locationId);
                    return new LocationNotFoundException(locationId);
                });
    }

    private User getUser(Long userId) {
        return UserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    return new UserNotFoundException(userId);
                });
    }
}
