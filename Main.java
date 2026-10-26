import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        DatabaseOperations.initTables();
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
                    break;
                }
            } else if (choice.equals("2")) {
                User newUser = handleAccountCreation(scanner);
                if (newUser != null) {
                    runUserSession(scanner, newUser);
                    break;
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

    private static User handleAccountCreation(Scanner scanner) {
        System.out.println("\n--- Account Creation ---");
        System.out.print("Enter a username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return null;
        }

        if (DatabaseOperations.findUser(username) != null) {
            System.out.println("That username is already taken. Please try again.");
            return null;
        }

        while (true) {
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            System.out.print("Confirm password: ");
            String confirmPassword = scanner.nextLine();

            if (!password.equals(confirmPassword)) {
                System.out.println("Passwords do not match. The password process is restarting.\n");
                continue;
            }

            User newUser = new User(username, password);
            if (DatabaseOperations.saveUser(newUser)) {
                System.out.println("Account created successfully! You are now logged in.");
                return newUser;
            } else {
                System.out.println("Could not create account due to a database error.");
                return null;
            }
        }
    }

    private static User handleLogin(Scanner scanner) {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = DatabaseOperations.authenticate(username, password);
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
        List<Message> userMessages = DatabaseOperations.loadMessagesFor(username);

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

        if (DatabaseOperations.findUser(recipient) == null) {
            System.out.println("Error: Recipient '" + recipient + "' does not exist.");
            return;
        }

        System.out.print("Enter your message: ");
        String content = scanner.nextLine();

        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        Message message = new Message(recipient, currentUser.getUsername(), timestamp, content);
        DatabaseOperations.saveMessage(message);

        System.out.println("Message sent successfully!");
    }
}
