package com.zwj.tool.handle;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.Properties;

/**
 * @author zhouwenjie
 * @version 1.0
 * @date 2025/1/23
 * @description TODO
 */
@Getter
public class HandleContext {

    private Connection connection;

    private SchemaPlus rootSchema;

    private CalciteConnection calciteConnection;

    private String fileUrl;

    private String schema;

    private String sql;

    @Builder
    public HandleContext(String fileUrl, String schema, String sql) {
        this.fileUrl = fileUrl;
        this.schema = schema;
        this.sql = sql;
        initResources();
    }

    @SneakyThrows
    private void initResources() {
        Properties info = new Properties();
        // 不区分sql大小写
        info.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), Boolean.FALSE.toString());

        // 创建Calcite连接
        connection = DriverManager.getConnection("jdbc:calcite:", info);
        calciteConnection = connection.unwrap(CalciteConnection.class);
        // 构建RootSchema，在Calcite中，RootSchema是所有数据源schema的parent，多个不同数据源schema可以挂在同一个RootSchema下
        rootSchema = calciteConnection.getRootSchema();
    }

    @SneakyThrows
    public void closeResources() {
        if (Objects.nonNull(connection)) {
            connection.close();
        }
    }

}
