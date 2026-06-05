package com.isd.wms.service;

import com.isd.wms.dto.product.ProductCreateRequest;
import com.isd.wms.dto.product.ProductResponse;
import com.isd.wms.dto.product.ProductUpdateRequest;
import com.isd.wms.entity.Category;
import com.isd.wms.entity.Product;
import com.isd.wms.exception.CategoryNotFoundException;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.exception.ProductNotFoundException;
import com.isd.wms.mapper.ProductMapper;
import com.isd.wms.repository.CategoryRepository;
import com.isd.wms.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating product: name={}, categoryId={}", request.name(), request.categoryId());
        validateProductRequest(request.name(), request.categoryId());
        Category category = getCategory(request.categoryId());
        Product product = new Product(request.name().trim(), request.description(), category);
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully: productId={}, categoryId={}", savedProduct.getId(), category.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        log.info("Updating product: productId={}, name={}, categoryId={}", productId, request.name(), request.categoryId());
        validateProductRequest(request.name(), request.categoryId());
        Product product = getProduct(productId);
        Category category = getCategory(request.categoryId());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);
        log.info("Product updated successfully: productId={}, categoryId={}", savedProduct.getId(), category.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        log.info("Deleting product: productId={}", productId);
        productRepository.delete(getProduct(productId));
        log.info("Product deleted successfully: productId={}", productId);
    }

    public ProductResponse getProductById(Long productId) {
        log.info("Getting product by id: productId={}", productId);
        Product product = getProduct(productId);
        log.debug("Product found: productId={}, categoryId={}", product.getId(), product.getCategory().getId());
        return productMapper.toResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        log.info("Getting all products");
        List<ProductResponse> products = productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
        log.info("Products fetched successfully: count={}", products.size());
        return products;
    }

    public List<ProductResponse> searchProducts(String name, Long categoryId) {
        log.info("Searching products: name={}, categoryId={}", name, categoryId);
        String searchName = name == null || name.isBlank() ? null : name.trim();
        if (searchName == null && categoryId == null) {
            log.warn("Invalid product search request: missing search parameters");
            throw new InvalidRequestException("At least one search parameter is required");
        }

        List<ProductResponse> products = productRepository.search(searchName, categoryId).stream()
                .map(productMapper::toResponse)
                .toList();
        log.info("Product search completed: name={}, categoryId={}, count={}", searchName, categoryId, products.size());
        return products;
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: productId={}", productId);
                    return new ProductNotFoundException(productId);
                });
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Category not found for product operation: categoryId={}", categoryId);
                    return new CategoryNotFoundException(categoryId);
                });
    }

    private void validateProductRequest(String name, Long categoryId) {
        if (name == null || name.isBlank()) {
            log.warn("Invalid product request: missing product name");
            throw new InvalidRequestException("Product name is required");
        }
        if (categoryId == null) {
            log.warn("Invalid product request: missing category");
            throw new InvalidRequestException("Product category is required");
        }
    }
}
