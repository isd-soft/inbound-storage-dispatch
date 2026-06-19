package com.isd.wms.service.imports.xlsx;

import com.isd.wms.enums.ImportType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.service.imports.ImportStrategy;
import com.poiji.bind.Poiji;
import com.poiji.exception.PoijiExcelType;
import com.poiji.exception.PoijiException;
import com.poiji.exception.PoijiMultiRowException;
import com.poiji.option.PoijiOptions;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Component
public class XlsxImportStrategy implements ImportStrategy {
    @Override
    public <T> List<T> parse(MultipartFile file, Class<T> clazz) {
        try (InputStream inputStream = file.getInputStream()) {

            PoijiOptions options = PoijiOptions.PoijiOptionsBuilder.settings()
                .preferNullOverDefault(true)
                .build();
            PoijiExcelType excelType = PoijiExcelType.XLSX;
            if (Objects.equals(FilenameUtils.getExtension(file.getOriginalFilename()), "xls")) {
                excelType = PoijiExcelType.XLS;
            }

            return Poiji.fromExcel(inputStream, excelType, clazz, options);

        } catch (PoijiMultiRowException.PoijiRowSpecificException e) {
            throw new InvalidRequestException("Excel format is wrong in the column " +
                e.getColumnName() + " on line " + e.getRowNum());
        } catch (PoijiException e) {
            throw new InvalidRequestException(e.getLocalizedMessage());
        } catch (Exception e) {
            throw new InvalidRequestException("An error occurred while parsing the file.");
        }
    }

    @Override
    public boolean support(ImportType type) {
        return type == ImportType.EXCEL;
    }
}
