package com.isd.wms.controller;

import com.isd.wms.dto.inventory.AddStockRequest;
import com.isd.wms.dto.inventory.AdjustStockRequest;
import com.isd.wms.dto.inventory.InventoryHistoryResponse;
import com.isd.wms.dto.inventory.RemoveStockRequest;
import com.isd.wms.dto.inventory.StockResponse;
import com.isd.wms.service.InventoryService;
import com.isd.wms.service.imports.ImportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ImportService importService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public List<StockResponse> getAllStock() {
        return inventoryService.getAllStock();
    }

    @GetMapping("/{stockId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public StockResponse getStockById(@PathVariable Long stockId) {
        return inventoryService.getStockById(stockId);
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<StockResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.addStock(request));
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public StockResponse removeStock(@Valid @RequestBody RemoveStockRequest request) {
        return inventoryService.removeStock(request);
    }

    @PutMapping("/adjust")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public StockResponse adjustStock(@Valid @RequestBody AdjustStockRequest request) {
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
        return inventoryService.getHistoryForStock(stockId);
    }

    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(("hasAnyRole('SUPERVISOR', 'DEV')"))
    public ResponseEntity<String> importStockFromFile(@RequestParam("file") MultipartFile file) {
        inventoryService.importStocksFromFile(file);
        return ResponseEntity.ok("Stocks were successfully imported.");
    }
}
