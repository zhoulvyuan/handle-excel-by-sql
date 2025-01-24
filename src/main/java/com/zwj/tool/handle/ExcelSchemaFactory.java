package com.zwj.tool.handle;

import com.google.common.collect.Lists;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * schema factory
 */
public class ExcelSchemaFactory implements SchemaFactory {
    public final static ExcelSchemaFactory INSTANCE = new ExcelSchemaFactory();

    private ExcelSchemaFactory(){}

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        final Object filePath = operand.get("filePath");

        if (ObjectUtils.isEmpty(filePath)) {
            throw new NullPointerException("can not find excel file");
        }

        return this.create(filePath.toString());
    }

    public Schema create(String excelFilePath) {
        if (StringUtils.isBlank(excelFilePath)) {
            throw new NullPointerException("can not find excel file");
        }

        return this.create(new File(excelFilePath));
    }

    public Schema create(File excelFile) {
        if (ObjectUtils.isEmpty(excelFile) || !excelFile.exists()) {
            throw new NullPointerException("can not find excel file");
        }

        if (!excelFile.isFile() || !isExcelFile(excelFile)) {
            throw new RuntimeException("can not find excel file: " + excelFile.getAbsolutePath());
        }

        return new ExcelSchema(excelFile);
    }

    protected List<String> supportedFileSuffix() {
        return Lists.newArrayList("xls", "xlsx");
    }

    private boolean isExcelFile(File excelFile) {
        if (ObjectUtils.isEmpty(excelFile)) {
            return false;
        }

        final String name = excelFile.getName();
        return StringUtils.endsWithAny(name, this.supportedFileSuffix().toArray(new String[0]));
    }
}