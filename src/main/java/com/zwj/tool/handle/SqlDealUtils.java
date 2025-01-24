package com.zwj.tool.handle;

import com.alibaba.fastjson2.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteResultSet;
import org.apache.calcite.schema.Schema;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

import java.io.FileOutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Objects;

/**
 * @author zhouwenjie
 * @version 1.0
 * @date 2025/1/23
 * @description sql处理工具类
 */
@Slf4j
public final class SqlDealUtils {

    private SqlDealUtils() {
    }

    private final static SqlDealUtils INSTANCE = new SqlDealUtils();

    public static SqlDealUtils getInstance() {
        return INSTANCE;
    }

    @SneakyThrows
    public synchronized void executeQueryWithOutPut(HandleContext context) {
        Statement statement = null;
        try {
            StopWatch stopWatch = new StopWatch("SqlDealUtils#executeQuery");
            stopWatch.start("开始执行文件解析任务");
            final Schema schema = ExcelSchemaFactory.INSTANCE.create(context.getFileUrl());
            context.getRootSchema().add(context.getSchema(), schema);
            // 设置默认的schema
            context.getCalciteConnection().setSchema(context.getSchema());
            statement = context.getCalciteConnection().createStatement();
            ResultSet resultSet = statement.executeQuery(context.getSql());
            stopWatch.stop();
            stopWatch.start("开始输出结果集");
            outPutResultSet(resultSet, gatherOutPath(context.getFileUrl()));
            stopWatch.stop();
            log.info(stopWatch.prettyPrint());
        } finally {
            context.closeResources();
            if (!Objects.isNull(statement)) {
                statement.close();
            }
        }
    }

    @SneakyThrows
    private void outPutResultSet(ResultSet resultSet, String fileUrl) {

        // 创建新的 Excel 文件
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Result");
        // 写入表头
        Row headerRow = sheet.createRow(0);
        ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(metaData.getColumnName(i + 1));
        }

        // 写入数据
        int rowNum = 1;
        while (resultSet.next()) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(resultSet.getString(i + 1));
            }
        }

        // 保存到文件
        try (FileOutputStream fileOut = new FileOutputStream(fileUrl)) {
            workbook.write(fileOut);
        }

        // 关闭资源
        resultSet.close();
        workbook.close();
    }

    /**
     * 创建输出路径
     * @param fileUrl
     * @return
     */
    private String gatherOutPath(String fileUrl) {
        Assert.hasText(fileUrl, "fileUrl can not be null");
        Assert.isTrue(fileUrl.endsWith(".xlsx"), "fileUrl must end with .xlsx");
        return fileUrl.substring(0, fileUrl.lastIndexOf(".xlsx")) + String.format("_%s_out.xlsx", System.currentTimeMillis());
    }
}
