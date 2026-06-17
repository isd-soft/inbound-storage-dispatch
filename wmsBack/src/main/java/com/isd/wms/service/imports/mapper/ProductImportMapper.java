package com.isd.wms.service.imports.mapper;

import com.isd.wms.entity.Category;
import com.isd.wms.entity.Product;
import com.isd.wms.entity.Stock;
import com.isd.wms.repository.CategoryRepository;
import com.isd.wms.repository.LocationRepository;
import com.isd.wms.repository.ProductRepository;
import com.isd.wms.service.imports.dto.ProductInfo;
import com.isd.wms.service.imports.dto.StockInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductImportMapper implements ImportMapper<ProductInfo, Product> {

    private final CategoryRepository categoryRepository;

    @Override
    public Product toEntity(ProductInfo info) {
        Category category = categoryRepository.findByNameIgnoreCase(info.getCategoryName())
            .orElse(categoryRepository.save(new Category(info.getCategoryName())));
        return new Product(
            info.getName(),
            info.getBarcode(),
            info.getDescription(),
            category
        );
    }

    @Override
    public Class<ProductInfo> supports() {
        return ProductInfo.class;
    }
}
