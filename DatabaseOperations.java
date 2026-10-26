import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseOperations {

    public static void initTables() {
        String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL
                );
                """;

        String createMessages = """
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    recipient VARCHAR(50) NOT NULL,
                    author VARCHAR(50) NOT NULL,
                    content TEXT NOT NULL,
                    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createMessages);
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    public static User findUser(String username) {
        String sql = "SELECT username, password FROM users WHERE LOWER(username) = LOWER(?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("username"), rs.getString("password"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }

    public static User authenticate(String username, String password) {
        String sql = "SELECT username, password FROM users WHERE LOWER(username) = LOWER(?) AND password = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getString("username"), rs.getString("password"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public static boolean saveUser(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
            return false;
        }
    }

    public static void saveMessage(Message message) {
        String sql = "INSERT INTO messages (recipient, author, content) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, message.getRecipient());
            stmt.setString(2, message.getAuthor());
            stmt.setString(3, message.getContent());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public static List<Message> loadMessagesFor(String username) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT recipient, author, COALESCE(TO_CHAR(sent_at, 'YYYY-MM-DD HH24:MI:SS'), '') AS formatted_time, content " +
                "FROM messages WHERE LOWER(recipient) = LOWER(?) ORDER BY sent_at ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                            rs.getString("recipient"),
                            rs.getString("author"),
                            rs.getString("formatted_time"),
                            rs.getString("content")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading messages: " + e.getMessage());
        }
        return messages;
    }
}