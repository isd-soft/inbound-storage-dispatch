package com.isd.wms.service.imports.csv;

import com.isd.wms.enums.ImportType;
import com.isd.wms.service.imports.ImportStrategy;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class CsvImportStrategy implements ImportStrategy {
    @Override
    public <T> List<T> parse(MultipartFile file, Class<T> clazz) {
        return List.of();
    }

    @Override
    public boolean support(ImportType type) {
        return false;
    }
}
