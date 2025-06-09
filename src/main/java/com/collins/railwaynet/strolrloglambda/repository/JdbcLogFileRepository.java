package com.collins.railwaynet.strolrloglambda.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.collins.railwaynet.strolrloglambda.entity.LogFile;

@Repository
public class JdbcLogFileRepository implements LogFileRepository {

    private static final String SQL_STATEMENT = "INSERT INTO logfileindex (mark, locoNumber, device, endTime, logFilePath) VALUES (?, ?, ?, ?, ?);";

    @Value("${database.url}")
    private String dbUrl;

    @Value("${database.writer}")
    private String dbUser;

    @Value("${database.password}")
    private String dbPassword;

    @Override
    public void save(LogFile logFile) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_STATEMENT)) {
            preparedStatement.setString(1, logFile.getMark());
            preparedStatement.setInt(2, logFile.getLocoNumber());
            preparedStatement.setString(3, logFile.getDevice());
            preparedStatement.setObject(4, logFile.getEndTime());
            preparedStatement.setString(5, logFile.getLogFilePath());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save log file to database", e);
        }
    }
}

