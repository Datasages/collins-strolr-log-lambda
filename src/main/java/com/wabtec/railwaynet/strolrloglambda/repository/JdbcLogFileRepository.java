// Copyright Wabtec Inc. 2025. All rights reserved
// @author Pete Kofod
package com.wabtec.railwaynet.strolrloglambda.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import static java.util.Objects.requireNonNull;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

import com.wabtec.railwaynet.strolrloglambda.entity.LogFile;
import com.wabtec.railwaynet.strolrloglambda.util.SecretManagerCache;

/**
 * JDBC repository that loads DB connection details from:
 * - env/Secrets for production (no-arg constructor)
 * - injected DataSource for tests/integration
 */
public class JdbcLogFileRepository implements LogFileRepository {

    private static final String SQL =
        "INSERT INTO logfileindex " +
        "(mark, locoNumber, device, endTime, logFilePath) " +
        "VALUES (?, ?, ?, ?, ?);";

    private final DataSource dataSource;

    /** Production constructor: reads DB URL/user from env, and password from AWS Secrets Manager */
    public JdbcLogFileRepository() {
        String url = requireEnv("DB_URL");
        String user = requireEnv("DB_USER");
        String password = requireEnv("DB_PASSWORD_SECRET_NAME");
        password = SecretManagerCache.getSecret(password);
        if (password == null) {
            throw new IllegalStateException("Cannot retrieve DB password from Secrets Manager");
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(url);
        ds.setUser(user);
        ds.setPassword(password);
        this.dataSource = ds;
    }

    /** Test/integration constructor: dataSource is injected directly */
    public JdbcLogFileRepository(DataSource dataSource) {
        this.dataSource = requireNonNull(dataSource, "DataSource must not be null");
    }

    @Override
    public void save(LogFile file) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, file.getMark());
            ps.setInt(2, file.getLocoNumber());
            ps.setString(3, file.getDevice());
            ps.setObject(4, file.getEndTime());
            ps.setString(5, file.getLogFilePath());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save LogFile to DB", e);
        }
    }

    private static String requireEnv(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return val;
    }
}
