package com.isd.wms.service.imports;

import com.isd.wms.enums.ImportType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImportStrategy {
    <T> List<T> parse(MultipartFile file, Class<T> clazz);

    boolean support(ImportType type);
}
