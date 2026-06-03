package com.isd.wms.service;

import com.isd.wms.dto.category.CategoryCreateRequest;
import com.isd.wms.dto.category.CategoryResponse;
import com.isd.wms.dto.category.CategoryUpdateRequest;
import com.isd.wms.entity.Category;
import com.isd.wms.exception.CategoryInUseException;
import com.isd.wms.exception.CategoryNotFoundException;
import com.isd.wms.exception.DuplicateCategoryNameException;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.mapper.CategoryMapper;
import com.isd.wms.repository.CategoryRepository;
import com.isd.wms.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CategoryMapper categoryMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        String name = validateAndNormalizeName(request.name());
        verifyUniqueName(name, null);
        return categoryMapper.toResponse(saveCategory(new Category(name), name));
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        Category category = getCategory(categoryId);
        String name = validateAndNormalizeName(request.name());
        verifyUniqueName(name, categoryId);
        category.setName(name);
        return categoryMapper.toResponse(saveCategory(category, name));
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getCategory(categoryId);
        if (productRepository.existsByCategoryId(categoryId)) {
            throw new CategoryInUseException(categoryId);
        }
        categoryRepository.delete(category);
    }

    public CategoryResponse getCategoryById(Long categoryId) {
        return categoryMapper.toResponse(getCategory(categoryId));
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private String validateAndNormalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Category name is required");
        }
        return name.trim();
    }

    private void verifyUniqueName(String name, Long categoryId) {
        categoryRepository.findByNameIgnoreCase(name)
                .filter(category -> !Objects.equals(category.getId(), categoryId))
                .ifPresent(category -> {
                    throw new DuplicateCategoryNameException(name);
                });
    }

    private Category saveCategory(Category category, String name) {
        try {
            return categoryRepository.save(category);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateCategoryNameException(name);
        }
    }
}
