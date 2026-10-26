import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Database {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = new FileInputStream("db.properties")) {
            PROPERTIES.load(input);
        } catch (IOException e) {
            System.err.println("Error reading db.properties: " + e.getMessage());
        }
    }

    private static String getSetting(String envVar, String propKey) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return PROPERTIES.getProperty(propKey, "");
    }

    public static Connection getConnection() throws SQLException {
        String url = getSetting("DB_URL", "db.url");
        String user = getSetting("DB_USER", "db.user");
        String password = getSetting("DB_PASSWORD", "db.password");

        if (url.isBlank()) {
            throw new SQLException("Database URL is not configured. Set environment variables (DB_URL, DB_USER, DB_PASSWORD) or configure db.properties.");
        }

        return DriverManager.getConnection(url, user, password);
    }
}
