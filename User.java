public class User {
    private final String username;
    private final String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String toFileString() {
        return username + "|" + password;
    }

    public static User fromFileString(String line) {
        String[] parts = line.split("\\|", 2);
        if (parts.length == 2) {
            return new User(parts[0], parts[1]);
        }
        return null;
    }
}