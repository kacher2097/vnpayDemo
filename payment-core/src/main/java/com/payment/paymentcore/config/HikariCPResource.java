package com.payment.paymentcore.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class HikariCPResource {
    private static HikariConfig config = new HikariConfig();

    private static HikariDataSource ds;

    static {
        config.setDriverClassName(DBConfig.DB_DRIVER);
        config.setJdbcUrl(DBConfig.CONNECTION_URL);
        config.setUsername(DBConfig.USER_NAME);
        config.setPassword(DBConfig.PASSWORD);
        config.setMinimumIdle(DBConfig.DB_MIN_CONNECTIONS);
        config.setMaximumPoolSize(DBConfig.DB_MAX_CONNECTIONS);

        // Some additional properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    private HikariCPResource() {
        super();
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

}
