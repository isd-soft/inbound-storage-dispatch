package com.isd.wms.service;

import com.isd.wms.dto.product.ProductCreateRequest;
import com.isd.wms.dto.product.ProductResponse;
import com.isd.wms.dto.product.ProductUpdateRequest;
import com.isd.wms.entity.Category;
import com.isd.wms.entity.Product;
import com.isd.wms.exception.CategoryNotFoundException;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.mapper.ProductMapper;
import com.isd.wms.repository.CategoryRepository;
import com.isd.wms.repository.ProductRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService productService;
    private Category category;
    private Validator validator;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, new ProductMapper());
        category = category(1L, "Dairy");
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createsProductSuccessfully() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(new ProductCreateRequest("Milk", "MILK-1", "Whole milk", 1L));

        assertThat(response.name()).isEqualTo("Milk");
        assertThat(response.barcode()).isEqualTo("MILK-1");
        assertThat(response.categoryId()).isEqualTo(1L);
    }

    @Test
    void rejectsProductWithoutName() {
        assertThat(validator.validate(new ProductCreateRequest(" ", "MILK-1", null, 1L)))
                .isNotEmpty();
    }

    @Test
    void rejectsProductWithoutCategory() {
        assertThat(validator.validate(new ProductCreateRequest("Milk", "MILK-1", null, null)))
                .isNotEmpty();
    }

    @Test
    void rejectsProductWithInvalidCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(new ProductCreateRequest("Milk", "MILK-1", null, 99L)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void getsProductById() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, "Milk", category)));

        assertThat(productService.getProductById(10L).name()).isEqualTo("Milk");
    }

    @Test
    void updatesProductSuccessfully() {
        Product product = product(10L, "Milk", category);
        Category drinks = category(2L, "Drinks");
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(drinks));
        when(productRepository.save(product)).thenReturn(product);

        ProductResponse response = productService.updateProduct(10L, new ProductUpdateRequest("Juice", "JUICE-1", null, 2L));

        assertThat(response.name()).isEqualTo("Juice");
        assertThat(response.barcode()).isEqualTo("JUICE-1");
        assertThat(response.categoryId()).isEqualTo(2L);
    }

    @Test
    void deletesProductSuccessfully() {
        Product product = product(10L, "Milk", category);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        productService.deleteProduct(10L);

        verify(productRepository).delete(product);
    }

    @Test
    void searchesProductsByName() {
        when(productRepository.search("milk", null))
                .thenReturn(List.of(product(10L, "Milk", category)));

        assertThat(productService.searchProducts("milk", null))
                .extracting(ProductResponse::name)
                .containsExactly("Milk");
    }

    @Test
    void filtersProductsByCategory() {
        when(productRepository.search(null, 1L))
                .thenReturn(List.of(product(10L, "Milk", category)));

        assertThat(productService.searchProducts(null, 1L))
                .extracting(ProductResponse::categoryId)
                .containsExactly(1L);
    }

    @Test
    void rejectsSearchWithoutParameters() {
        assertThatThrownBy(() -> productService.searchProducts(" ", null))
                .isInstanceOf(InvalidRequestException.class);
    }

    private Category category(Long id, String name) {
        Category result = new Category(name);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private Product product(Long id, String name, Category productCategory) {
        Product result = new Product(name, "SKU-" + id, null, productCategory);
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}
