package com.isd.wms.controller;

import com.isd.wms.dto.category.CategoryCreateRequest;
import com.isd.wms.dto.category.CategoryResponse;
import com.isd.wms.dto.category.CategoryUpdateRequest;
import com.isd.wms.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/imports")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'DEV')")
    public ResponseEntity<String> importCategories(@RequestParam("file") MultipartFile file) {
        categoryService.importCategoriesFromFile(file);
        return ResponseEntity.ok("Categories were successfully imported.");
    }
}
