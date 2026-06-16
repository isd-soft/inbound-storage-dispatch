package com.isd.wms.service.imports.xlsx;

import com.isd.wms.enums.ImportType;
import com.isd.wms.exception.InvalidRequestException;
import com.isd.wms.service.imports.ImportStrategy;
import com.poiji.bind.Poiji;
import com.poiji.exception.PoijiExcelType;
import com.poiji.exception.PoijiException;
import com.poiji.exception.PoijiMultiRowException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public class XlsxImportStrategy implements ImportStrategy {
    @Override
    public <T> List<T> parse(MultipartFile file, Class<T> clazz) {
        try (InputStream inputStream = file.getInputStream()) {

            PoijiExcelType excelType = PoijiExcelType.XLSX;
            if (FilenameUtils.getExtension(file.getOriginalFilename()).equals("xls")) {
                excelType = PoijiExcelType.XLS;
            }

            return Poiji.fromExcel(inputStream, excelType, clazz);

        } catch (PoijiMultiRowException.PoijiRowSpecificException e) {
            throw new InvalidRequestException("Excel format is wrong in the column " +
                e.getColumnName() + " on line " + e.getRowNum());
        } catch (PoijiException e) {
            throw new InvalidRequestException(e.getLocalizedMessage());
        } catch (Exception e) {
            throw new InvalidRequestException(e.getLocalizedMessage());
        }
    }

    @Override
    public boolean support(ImportType type) {
        return type == ImportType.EXCEL;
    }
}
