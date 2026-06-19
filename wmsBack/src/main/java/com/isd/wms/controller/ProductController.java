package com.isd.wms.controller;

import com.isd.wms.dto.product.ProductCreateRequest;
import com.isd.wms.dto.product.ProductResponse;
import com.isd.wms.dto.product.ProductUpdateRequest;
import com.isd.wms.enums.Zone;
import com.isd.wms.repository.projections.ProductWithQuantityProjection;
import com.isd.wms.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;


    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        log.info("Create product request: name={}, categoryId={}", request.name(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        log.info("Get all products request");
        return productService.getAllProducts();
    }

    @GetMapping("/quantities")
    public List<ProductWithQuantityProjection> getAllProductsWithQuantity(@RequestParam Zone zone) {
        return productService.getAllProductsWithQuantity(zone);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        log.info("Get product by id request: productId={}", id);
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        log.info("Update product request: productId={}, name={}, categoryId={}", id, request.name(), request.categoryId());
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Delete product request: productId={}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId
    ) {
        log.info("Search products request: name={}, categoryId={}", name, categoryId);
        return productService.searchProducts(name, categoryId);
    }

    @PostMapping("/imports")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> importProducts(@RequestParam("file") MultipartFile file) {
        productService.importProductsFromFile(file);
        return ResponseEntity.ok("Products were successfully imported.");
    }
}
