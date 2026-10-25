import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Path USERS_FILE = Paths.get("users.txt");
    private static final Path MESSAGES_FILE = Paths.get("messages.txt");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static void main() {
        initStorage();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Welcome to Hush ===");
            System.out.println("1. Login");
            System.out.println("2. Create Account");
            System.out.println("3. Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            if (choice.equals("1")) {
                User loggedInUser = handleLogin(scanner);
                if (loggedInUser != null) {
                    runUserSession(scanner, loggedInUser);
                    break; // Program ends when user logs out
                }
            } else if (choice.equals("2")) {
                User newUser = handleAccountCreation(scanner);
                if (newUser != null) {
                    runUserSession(scanner, newUser);
                    break; // Program ends when user logs out
                }
            } else if (choice.equals("3")) {
                System.out.println("Farewell!");
                break;
            } else {
                System.out.println("Invalid selection. Please try again.");
            }
        }

        scanner.close();
    }

    private static void initStorage() {
        try {
            if (!Files.exists(USERS_FILE)) {
                Files.createFile(USERS_FILE);
            }
            if (!Files.exists(MESSAGES_FILE)) {
                Files.createFile(MESSAGES_FILE);
            }
        } catch (IOException e) {
            System.err.println("Could not initialize storage files: " + e.getMessage());
        }
    }

    private static User handleAccountCreation(Scanner scanner) {
        System.out.println("\n--- Account Creation ---");
        System.out.print("Enter a username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return null;
        }

        if (username.contains("|")) {
            System.out.println("Usernames cannot contain the '|' character.");
            return null;
        }

        if (findUser(username) != null) {
            System.out.println("That username is already taken. Please try again.");
            return null;
        }

        while (true) {
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            System.out.print("Confirm password: ");
            String confirmPassword = scanner.nextLine();

            if (password.contains("|")) {
                System.out.println("Passwords cannot contain the '|' character. Please choose another.");
                continue;
            }

            if (!password.equals(confirmPassword)) {
                System.out.println("Passwords do not match. The password process is restarting.\n");
                continue;
            }

            User newUser = new User(username, password);
            saveUser(newUser);
            System.out.println("Account created successfully! You are now logged in.");
            return newUser;
        }
    }

    private static User handleLogin(Scanner scanner) {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = authenticate(username, password);
        if (user != null) {
            System.out.println("Login successful! Welcome back, " + user.getUsername() + ".");
            return user;
        } else {
            System.out.println("Login failed: Username or password does not match.");
            return null;
        }
    }

    private static void runUserSession(Scanner scanner, User currentUser) {
        while (true) {
            System.out.println("\n=== Dashboard (User: " + currentUser.getUsername() + ") ===");
            System.out.println("1. Read messages");
            System.out.println("2. Send message");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> readMessagesFor(currentUser.getUsername());
                case "2" -> sendMessage(scanner, currentUser);
                case "3" -> {
                    System.out.println("Farewell!");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void readMessagesFor(String username) {
        System.out.println("\n--- Your Inbox ---");
        List<Message> allMessages = loadMessages();
        List<Message> userMessages = new ArrayList<>();

        for (Message msg : allMessages) {
            if (msg.getRecipient().equalsIgnoreCase(username)) {
                userMessages.add(msg);
            }
        }

        if (userMessages.isEmpty()) {
            System.out.println("No messages found.");
        } else {
            for (Message msg : userMessages) {
                System.out.println("----------------------------------------");
                System.out.println("From:    " + msg.getAuthor());
                System.out.println("Date:    " + msg.getTimestamp());
                System.out.println("Message: " + msg.getContent());
            }
            System.out.println("----------------------------------------");
        }
    }

    private static void sendMessage(Scanner scanner, User currentUser) {
        System.out.println("\n--- Send Message ---");
        System.out.print("Recipient username: ");
        String recipient = scanner.nextLine().trim();

        if (findUser(recipient) == null) {
            System.out.println("Error: Recipient '" + recipient + "' does not exist.");
            return;
        }

        System.out.print("Enter your message: ");
        String content = scanner.nextLine();

        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        Message message = new Message(recipient, currentUser.getUsername(), timestamp, content);
        saveMessage(message);

        System.out.println("Message sent successfully!");
    }

    private static List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        if (!Files.exists(USERS_FILE)) {
            return users;
        }
        try {
            List<String> lines = Files.readAllLines(USERS_FILE);
            for (String line : lines) {
                if (!line.isBlank()) {
                    User user = User.fromFileString(line);
                    if (user != null) {
                        users.add(user);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading users: " + e.getMessage());
        }
        return users;
    }

    private static User findUser(String username) {
        for (User user : loadUsers()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    private static User authenticate(String username, String password) {
        for (User user : loadUsers()) {
            if (user.getUsername().equalsIgnoreCase(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    private static void saveUser(User user) {
        try {
            Files.writeString(
                    USERS_FILE,
                    user.toFileString() + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Error saving user: " + e.getMessage());
        }
    }

    private static List<Message> loadMessages() {
        List<Message> messages = new ArrayList<>();
        if (!Files.exists(MESSAGES_FILE)) {
            return messages;
        }
        try {
            List<String> lines = Files.readAllLines(MESSAGES_FILE);
            for (String line : lines) {
                if (!line.isBlank()) {
                    Message msg = Message.fromFileString(line);
                    if (msg != null) {
                        messages.add(msg);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading messages: " + e.getMessage());
        }
        return messages;
    }

    private static void saveMessage(Message message) {
        try {
            Files.writeString(
                    MESSAGES_FILE,
                    message.toFileString() + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }
}