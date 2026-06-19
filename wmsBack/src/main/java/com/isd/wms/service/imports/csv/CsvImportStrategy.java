package com.isd.wms.service.imports.csv;

import com.isd.wms.enums.ImportType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.service.imports.ImportStrategy;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class CsvImportStrategy implements ImportStrategy {
    @Override
    public <T> List<T> parse(MultipartFile file, Class<T> clazz) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {

            return new CsvToBeanBuilder<T>(reader)
                .withType(clazz)
                .withIgnoreLeadingWhiteSpace(true)
                .build()
                .parse();

        } catch (Exception e) {
            throw new InvalidRequestException("An error occurred at parsing .csv file.");
        }
    }

    @Override
    public boolean support(ImportType type) {
        return type == ImportType.CSV;
    }
}
