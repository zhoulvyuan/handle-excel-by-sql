package com.zwj.tool.constant;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.util.DateTimeUtils;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;

/**
 * type converter
 */
@Slf4j
@Getter
public enum JavaFileTypeEnum {
    STRING("string", SqlTypeName.VARCHAR, Cell::getStringCellValue),
    BOOLEAN("boolean", SqlTypeName.BOOLEAN, Cell::getBooleanCellValue),
    BYTE("byte", SqlTypeName.TINYINT, Cell::getStringCellValue),
    CHAR("char", SqlTypeName.CHAR, Cell::getStringCellValue),
    SHORT("short", SqlTypeName.SMALLINT, Cell::getNumericCellValue),
    INT("int", SqlTypeName.INTEGER, cell -> (Double.valueOf(cell.getNumericCellValue()).intValue())),
    LONG("long", SqlTypeName.BIGINT, cell -> (Double.valueOf(cell.getNumericCellValue()).longValue())),
    FLOAT("float", SqlTypeName.REAL, Cell::getNumericCellValue),
    DOUBLE("double", SqlTypeName.DOUBLE, Cell::getNumericCellValue),
    DATE("date", SqlTypeName.DATE, getValueWithDate()),
    TIMESTAMP("timestamp", SqlTypeName.TIMESTAMP, getValueWithTimestamp()),
    TIME("time", SqlTypeName.TIME, getValueWithTime()),
    UNKNOWN("unknown", SqlTypeName.UNKNOWN, getValueWithUnknown()),;
    // cell type
    private final String typeName;
	// sql type
    private final SqlTypeName sqlTypeName;
    // value convert func
    private final Function<Cell, Object> cellValueFunc;

    private static final FastDateFormat TIME_FORMAT_DATE;

    private static final FastDateFormat TIME_FORMAT_TIME;

    private static final FastDateFormat TIME_FORMAT_TIMESTAMP;

    static {
        final TimeZone gmt = TimeZone.getTimeZone("GMT");
        TIME_FORMAT_DATE = FastDateFormat.getInstance("yyyy-MM-dd", gmt);
        TIME_FORMAT_TIME = FastDateFormat.getInstance("HH:mm:ss", gmt);
        TIME_FORMAT_TIMESTAMP = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss", gmt);
    }

    JavaFileTypeEnum(String typeName, SqlTypeName sqlTypeName, Function<Cell, Object> cellValueFunc) {
        this.typeName = typeName;
        this.sqlTypeName = sqlTypeName;
        this.cellValueFunc = cellValueFunc;
    }

    public static Optional<JavaFileTypeEnum> of(String typeName) {
        return Arrays
                .stream(values())
                .filter(type -> StringUtils.equalsIgnoreCase(typeName, type.getTypeName()))
                .findFirst();
    }

    public static SqlTypeName findSqlTypeName(String typeName) {
        final Optional<JavaFileTypeEnum> javaFileTypeOptional = of(typeName);

        if (javaFileTypeOptional.isPresent()) {
            return javaFileTypeOptional
                    .get()
                    .getSqlTypeName();
        }

        return SqlTypeName.UNKNOWN;
    }

    public Object getCellValue(Cell cell) {
        return cellValueFunc.apply(cell);
    }

    public static Function<Cell, Object> getValueWithUnknown() {
        return cell -> {
            if (ObjectUtils.isEmpty(cell)) {
                return null;
            }

            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // 如果是日期类型，返回日期对象
                        return cell.getDateCellValue();
                    }
                    else {
                        // 否则返回数值
                        return cell.getNumericCellValue();
                    }
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                case FORMULA:
                    // 对于公式单元格，先计算公式结果，再获取其值
                    try {
                        return cell.getNumericCellValue();
                    }
                    catch (Exception e) {
                        try {
                            return cell.getStringCellValue();
                        }
                        catch (Exception ex) {
                            log.error("parse unknown data error, cellRowIndex:{}, cellColumnIndex:{}", cell.getRowIndex(), cell.getColumnIndex(), e);
                            return null;
                        }
                    }
                case BLANK:
                    return "";
                default:
                    return null;
            }
        };
    }

    public static Function<Cell, Object> getValueWithDate() {
        return cell -> {
            Date date = cell.getDateCellValue();

            if(ObjectUtils.isEmpty(date)) {
                return null;
            }

            try {
                final String formated = new SimpleDateFormat("yyyy-MM-dd").format(date);
                Date newDate = TIME_FORMAT_DATE.parse(formated);
                return (int) (newDate.getTime() / DateTimeUtils.MILLIS_PER_DAY);
            }
            catch (ParseException e) {
                log.error("parse date error, date:{}", date, e);
            }

            return null;
        };
    }

    public static Function<Cell, Object> getValueWithTimestamp() {
        return cell -> {
            Date date = cell.getDateCellValue();

            if(ObjectUtils.isEmpty(date)) {
                return null;
            }

            try {
                final String formated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                Date newDate = TIME_FORMAT_TIMESTAMP.parse(formated);
                return (int) newDate.getTime();
            }
            catch (ParseException e) {
                log.error("parse timestamp error, date:{}", date, e);
            }

            return null;
        };
    }

    public static Function<Cell, Object> getValueWithTime() {
        return cell -> {
            Date date = cell.getDateCellValue();

            if(ObjectUtils.isEmpty(date)) {
                return null;
            }

            try {
                final String formated = new SimpleDateFormat("HH:mm:ss").format(date);
                Date newDate = TIME_FORMAT_TIME.parse(formated);
                return newDate.getTime();
            }
            catch (ParseException e) {
                log.error("parse time error, date:{}", date, e);
            }

            return null;
        };
    }
}