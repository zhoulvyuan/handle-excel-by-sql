package com.zwj.tool.handle;

import com.google.common.collect.Maps;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

/**
 * schema
 */
public class ExcelSchema extends AbstractSchema {
    private final File excelFile;

    private Map<String, Table> tableMap;

    public ExcelSchema(File excelFile) {
        this.excelFile = excelFile;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        if (ObjectUtils.isEmpty(tableMap)) {
            tableMap = createTableMap();
        }

        return tableMap;
    }

    private Map<String, Table> createTableMap() {
        final Map<String, Table> result = Maps.newHashMap();

        try (Workbook workbook = WorkbookFactory.create(excelFile)) {
            final Iterator<Sheet> sheetIterator = workbook.sheetIterator();

            while (sheetIterator.hasNext()) {
                final Sheet sheet = sheetIterator.next();
                final ExcelScannableTable excelScannableTable = new ExcelScannableTable(sheet, null);
                result.put(sheet.getSheetName(), excelScannableTable);
            }
        }
        catch (Exception ignored) {}

        return result;
    }
}