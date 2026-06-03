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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
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
        validateProductRequest(request.name(), request.categoryId());
        Category category = getCategory(request.categoryId());
        Product product = new Product(request.name().trim(), request.description(), category);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        validateProductRequest(request.name(), request.categoryId());
        Product product = getProduct(productId);
        Category category = getCategory(request.categoryId());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(category);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long productId) {
        productRepository.delete(getProduct(productId));
    }

    public ProductResponse getProductById(Long productId) {
        return productMapper.toResponse(getProduct(productId));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> searchProducts(String name, Long categoryId) {
        boolean hasName = name != null && !name.isBlank();
        List<Product> products;
        if (hasName && categoryId != null) {
            products = productRepository.findByNameContainingIgnoreCaseAndCategoryId(name.trim(), categoryId);
        } else if (hasName) {
            products = productRepository.findByNameContainingIgnoreCase(name.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(productMapper::toResponse).toList();
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private void validateProductRequest(String name, Long categoryId) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Product name is required");
        }
        if (categoryId == null) {
            throw new InvalidRequestException("Product category is required");
        }
    }
}
