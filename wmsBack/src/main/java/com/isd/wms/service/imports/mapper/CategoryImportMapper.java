package com.isd.wms.service.imports.mapper;

import com.isd.wms.entity.Category;
import com.isd.wms.service.imports.xlsx.dto.CategoryInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryImportMapper implements ImportMapper<CategoryInfo, Category> {

    @Override
    public Category toEntity(CategoryInfo info) {
        return new Category(info.categoryName());
    }

    @Override
    public Class<CategoryInfo> supports() {
        return CategoryInfo.class;
    }
}
