package com.isd.wms.service;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.dto.inventory.AdjustStockRequest;
import com.isd.wms.dto.inventory.InventoryAdjustmentRequest;
import com.isd.wms.dto.inventory.InventoryAdjustmentResponse;
import com.isd.wms.dto.inventory.InventoryHistoryResponse;
import com.isd.wms.dto.inventory.RemoveStockRequest;
import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.entity.InventoryHistory;
import com.isd.wms.enums.InventoryOperationType;
import com.isd.wms.entity.Location;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.entity.User;
import com.isd.wms.enums.Zone;
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
import com.isd.wms.repository.AllocationRepository;
import com.isd.wms.repository.StockRepository;
import com.isd.wms.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import com.isd.wms.service.imports.ImportService;
import com.isd.wms.service.imports.xlsx.dto.StockInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final StockRepository stockRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final AllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final StockMapper stockMapper;
    private final InventoryHistoryMapper inventoryHistoryMapper;
    private final ReplenishmentService replenishmentService;
    private final ImportService importService;
    private final InventoryAdjustmentService inventoryAdjustmentService;

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
            request.productId(), request.locationId(), request.quantity(), request.userId());

        Product product = getProduct(request.productId());
        Location location = getLocation(request.locationId());
        User user = getUser(request.userId());

        Stock stock = stockRepository.findByProductIdAndLocationId(product.getId(), location.getId())
            .orElseGet(() -> new Stock(product, location));

        Integer reservedQuantity = request.quantity() == null ? 0 : request.quantity();
        stock.setQuantity(stock.getQuantity() + reservedQuantity);
        stock.setReservedQuantity(stock.getReservedQuantity() + request.reservedQuantity());
        stock.setManufactureDate(request.manufactureDate());
        stock.setExpirationDate(request.expirationDate());

        Stock savedStock = stockRepository.save(stock);

        createHistory(savedStock, request.quantity(), savedStock.getQuantity(), null, location,
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

        triggerReplenishmentCheck(savedStock);

        log.info("Stock removed successfully: stockId={}, finalQuantity={}", savedStock.getId(), finalQuantity);
        return stockMapper.toResponse(savedStock);
    }

    @Transactional
    public StockResponse adjustStock(AdjustStockRequest request) {
        InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(
            request.getStockId(),
            new InventoryAdjustmentRequest(
                request.getNewQuantity(),
                request.getUserId(),
                request.getReason(),
                request.getComment(),
                request.getManufactureDate(),
                request.getExpirationDate()
            )
        );
        return response.stock();
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
                stock.getQuantity() - alteredQuantity,
                sourceLocation,
                destinationLocation,
                operationType,
                null,
                null,
                user
        );
        history.setTimestamp(LocalDateTime.now());
        inventoryHistoryRepository.save(history);
    }

    private void triggerReplenishmentCheck(Stock stock) {
        Product product = stock.getProduct().orElse(null);
        if (product == null) return;

        if (stock.getLocation().getZone() != Zone.PICKING) {
            return;
        }
        int locationQty = stock.getQuantity() - stock.getReservedQuantity();

        replenishmentService.checkAndTriggerAutoReplenishment(product, stock.getLocation(), locationQty);
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
        return userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found: userId={}", userId);
                return new UserNotFoundException(userId);
            });
    }

    @Transactional
    public void importStocksFromFile(MultipartFile file) {
        List<AddStockRequest> stocks = importService.importData(file, StockInfo.class);
        try {
            for(AddStockRequest stockRequest: stocks) {
                addStock(stockRequest);
            }
        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("The imported file contains invalid stock data.");
        }
    }
}
