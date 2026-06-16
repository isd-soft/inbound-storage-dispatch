package com.isd.wms.service.imports;

import com.isd.wms.enums.ImportType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.service.imports.mapper.ImportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final List<ImportStrategy> importStrategies;
    private final List<ImportMapper<?, ?>> mappers;

    @SuppressWarnings("unchecked")
    private <T, E> ImportMapper<T, E> resolveMapper(Class<T> clazz) {
        return (ImportMapper<T, E>) mappers.stream()
            .filter(m -> m.supports().equals(clazz))
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException("No mapper found for class: " + clazz.getName())
            );
    }

    public <T, E> List<E> importData(MultipartFile file, Class<T> clazz) {

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new InvalidRequestException("File name is missing");
        }

        String extension = FilenameUtils.getExtension(filename).toLowerCase();

        ImportType type = switch (extension) {
            case "xls", "xlsx" -> ImportType.EXCEL;
            case "csv" -> ImportType.CSV;
            default -> throw new InvalidRequestException("Unsupported file type: " + extension);
        };

        ImportStrategy strategy = importStrategies.stream()
            .filter(s -> s.support(type))
            .findFirst()
            .orElseThrow(() ->
                new InvalidRequestException("No import strategy for type: " + type)
            );

        List<T> dtos = strategy.parse(file, clazz);

        ImportMapper<T, E> mapper = resolveMapper(clazz);

        return dtos.stream()
            .map(mapper::toEntity)
            .toList();
    }
}
