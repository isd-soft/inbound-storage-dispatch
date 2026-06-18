package com.isd.wms.service.imports.mapper;

import com.isd.wms.entity.Category;
import com.isd.wms.entity.Product;
import com.isd.wms.repository.CategoryRepository;
import com.isd.wms.service.imports.xlsx.dto.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductImportMapper implements ImportMapper<ProductInfo, Product> {

    private final CategoryRepository categoryRepository;

    @Override
    public Product toEntity(ProductInfo info) {
        Category category = categoryRepository.findByNameIgnoreCase(info.categoryName())
            .orElse(categoryRepository.save(new Category(info.categoryName())));
        return new Product(
            info.name(),
            info.barcode(),
            info.description(),
            category
        );
    }

    @Override
    public Class<ProductInfo> supports() {
        return ProductInfo.class;
    }
}
