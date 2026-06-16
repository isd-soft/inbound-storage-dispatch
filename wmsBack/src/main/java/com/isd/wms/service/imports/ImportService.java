package com.isd.wms.service.imports;

import com.isd.wms.enums.ImportType;
import com.isd.wms.service.imports.mapper.ImportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final List<ImportStrategy> importStrategies;
    private final List<ImportMapper<?, ?>> mappers;

    private <T, E> ImportMapper<T, E> resolveMapper(Class<T> clazz) {
        return (ImportMapper<T, E>) mappers.stream()
            .filter(m -> m.supports().equals(clazz))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No mapper for: " + clazz));
    }

    public <T, E> List<E> importData(MultipartFile file, Class<T> clazz) {

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        ImportType type = switch (extension) {
            case "xls", "xlsx" -> ImportType.EXCEL;
            case "csv" -> ImportType.CSV;
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };

        ImportStrategy strategy = importStrategies.stream()
            .filter(s -> s.support(type))
            .findFirst()
            .orElseThrow();

        List<T> dtos = strategy.parse(file, clazz);

        ImportMapper<T, E> mapper = resolveMapper(clazz);

        return dtos.stream()
            .map(mapper::toEntity)
            .toList();
    }
}
