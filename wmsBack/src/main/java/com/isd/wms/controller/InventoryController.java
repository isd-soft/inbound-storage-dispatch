package com.isd.wms.controller;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.dto.inventory.AdjustStockRequest;
import com.isd.wms.dto.inventory.InventoryHistoryResponse;
import com.isd.wms.dto.inventory.RemoveStockRequest;
import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<StockResponse> getAllStock() {
        return inventoryService.getAllStock();
    }

    @GetMapping("/{stockId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public StockResponse getStockById(@PathVariable Long stockId) {
        log.info("REST request to get Stock details for ID: {}", stockId);
        return inventoryService.getStockById(stockId);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<StockResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        log.info("REST request to ADD stock. Product ID: {}, Quantity: {}", request.getProductId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addStock(request));
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public StockResponse removeStock(@Valid @RequestBody RemoveStockRequest request) {
        log.info("REST request to REMOVE stock. Product ID: {}, Quantity: {}", request.getStockId(), request.getQuantity());
        return inventoryService.removeStock(request);
    }

    @PutMapping("/adjust")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public StockResponse adjustStock(@Valid @RequestBody AdjustStockRequest request) {
        log.info("REST request to ADJUST stock. Stock ID: {}, New Quantity: {}",
            request.getStockId(), request.getNewQuantity());
        return inventoryService.adjustStock(request);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<InventoryHistoryResponse> getAllHistory() {
        return inventoryService.getAllHistory();
    }

    @GetMapping("/{stockId}/history")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<InventoryHistoryResponse> getStockHistory(@PathVariable Long stockId) {
        log.info("REST request to get Inventory History for Stock ID: {}", stockId);
        return inventoryService.getHistoryForStock(stockId);
    }
}
