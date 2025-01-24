package com.zwj.tool.handle;

import com.google.common.collect.Lists;
import com.zwj.tool.constant.JavaFileTypeEnum;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * table
 */
public class ExcelScannableTable extends AbstractTable implements ScannableTable {
    private final RelProtoDataType protoRowType;

    private final Sheet sheet;

    private RelDataType rowType;

    private List<JavaFileTypeEnum> fieldTypes;

    private List<Object[]> rowDataList;

    public ExcelScannableTable(Sheet sheet, RelProtoDataType protoRowType) {
        this.protoRowType = protoRowType;
        this.sheet = sheet;
    }

    @Override
    public Enumerable<@Nullable Object[]> scan(DataContext root) {
        JavaTypeFactory typeFactory = root.getTypeFactory();
        final List<JavaFileTypeEnum> fieldTypes = this.getFieldTypes(typeFactory);

        if (rowDataList == null) {
            rowDataList = readExcelData(sheet, fieldTypes);
        }

        return Linq4j.asEnumerable(rowDataList);
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        if (ObjectUtils.isNotEmpty(protoRowType)) {
            return protoRowType.apply(typeFactory);
        }

        if (ObjectUtils.isEmpty(rowType)) {
            rowType = deduceRowType((JavaTypeFactory) typeFactory, sheet, null);
        }

        return rowType;
    }

    public List<JavaFileTypeEnum> getFieldTypes(RelDataTypeFactory typeFactory) {
        if (fieldTypes == null) {
            fieldTypes = Lists.newArrayList();
            deduceRowType((JavaTypeFactory) typeFactory, sheet, fieldTypes);
        }
        return fieldTypes;
    }

    private List<Object[]> readExcelData(Sheet sheet, List<JavaFileTypeEnum> fieldTypes) {
        List<Object[]> rowDataList = Lists.newArrayList();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            Object[] rowData = new Object[fieldTypes.size()];

            for (int i = 0; i < row.getLastCellNum(); i++) {
                final JavaFileTypeEnum javaFileTypeEnum = fieldTypes.get(i);
                Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                final Object cellValue = javaFileTypeEnum.getCellValue(cell);
                rowData[i] = cellValue;
            }

            rowDataList.add(rowData);
        }

        return rowDataList;
    }

    public static RelDataType deduceRowType(JavaTypeFactory typeFactory, Sheet sheet, List<JavaFileTypeEnum> fieldTypes) {
        final List<String> names = Lists.newArrayList();
        final List<RelDataType> types = Lists.newArrayList();

        if (sheet != null) {
            Row headerRow = sheet.getRow(0);

            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String[] columnInfo = cell
                            .getStringCellValue()
                            .split(":");
                    String columnName = columnInfo[0].trim();
                    String columnType = null;

                    if (columnInfo.length == 2) {
                        columnType = columnInfo[1].trim();
                    }

                    final JavaFileTypeEnum javaFileType = JavaFileTypeEnum
                            .of(columnType)
                            .orElse(JavaFileTypeEnum.UNKNOWN);
                    final RelDataType sqlType = typeFactory.createSqlType(javaFileType.getSqlTypeName());
                    names.add(columnName);
                    types.add(sqlType);

                    if (fieldTypes != null) {
                        fieldTypes.add(javaFileType);
                    }
                }
            }
        }

        if (names.isEmpty()) {
            names.add("line");
            types.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
        }

        return typeFactory.createStructType(Pair.zip(names, types));
    }
}