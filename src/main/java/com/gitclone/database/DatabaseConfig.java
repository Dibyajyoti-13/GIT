package com.gitclone.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and manages database configuration properties.
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Database properties loaded successfully.");
            } else {
                logger.warn("db.properties not found in classpath. Falling back to default properties.");
                properties.setProperty("db.url", "jdbc:mariadb://localhost:3306/git_project");
                properties.setProperty("db.user", "user");
                properties.setProperty("db.password", "123456");
            }
        } catch (IOException e) {
            logger.error("Failed to load db.properties file", e);
        }
    }

    public static String getUrl() {
        return properties.getProperty("db.url");
    }

    public static String getUser() {
        return properties.getProperty("db.user");
    }

    public static String getPassword() {
        return properties.getProperty("db.password");
    }
}
