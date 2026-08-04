package com.gitclone.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Manages SQL connections to the MariaDB database.
 */
public class DatabaseConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);

    static {
        try {
            // Explicitly load MariaDB driver
            Class.forName("org.mariadb.jdbc.Driver");
            logger.info("MariaDB JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            logger.error("MariaDB JDBC Driver not found in classpath.", e);
        }
    }

    /**
     * Obtains a connection to the database.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        String url = DatabaseConfig.getUrl();
        String user = DatabaseConfig.getUser();
        String password = DatabaseConfig.getPassword();
        logger.debug("Connecting to database: {}", url);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Initializes the database schema from the schema.sql resource file.
     *
     * @throws SQLException if database error occurs
     */
    public static void initializeSchema() throws SQLException {
        try (InputStream schemaStream = DatabaseConnectionManager.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (schemaStream == null) {
                throw new IllegalStateException("schema.sql file not found in classpath.");
            }
            String schemaSql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream))) {
                schemaSql = reader.lines().collect(Collectors.joining("\n"));
            }

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                // Split SQL by semicolon, but do it carefully.
                // For simplicity, we execute statements split by standard semicolon lines
                String[] queries = schemaSql.split(";");
                for (String query : queries) {
                    if (!query.trim().isEmpty()) {
                        logger.debug("Executing setup query: {}", query.trim());
                        stmt.execute(query.trim());
                    }
                }
                logger.info("Database schema initialized successfully.");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Failed to load schema resource", e);
            }
        }
    }
}
