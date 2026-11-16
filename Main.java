import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long POLL_INTERVAL_NANOS = 1_000_000_000L;
    private static final Object CONSOLE_LOCK = new Object();

    // Entry point function
    public static void main(String[] args) {
        DatabaseOperations.initTables();


        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== Welcome to Hush ===");
                System.out.println("1. Login");
                System.out.println("2. Create Account");
                System.out.println("3. Exit");
                System.out.print("Select an option: ");

                if (!scanner.hasNextLine()) {
                    return;
                }

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
            }

            System.out.println("Could not create account due to a database error.");
            return null;
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
        }

        System.out.println("Login failed: Username or password does not match.");
        return null;
    }

    /**
     * Prints the user's existing inbox, then polls for newer messages once per second.
     * A separate input thread handles outgoing messages and logout commands.
     */
    private static void runUserSession(Scanner scanner, User currentUser) {
        String username = currentUser.getUsername();
        long lastPrintedMessageId = 0L;

        synchronized (CONSOLE_LOCK) {
            System.out.println("\n=== Hush (User: " + username + ") ===");
            System.out.println("Send a message using: content|recipient");
            System.out.println("You may also use: \"content\"|\"recipient\"");
            System.out.println("Type logout to end your session.");
            System.out.println("\n--- Messages addressed to you ---");
        }

        // Print every message already addressed to this user at login.
        List<Message> existingMessages = DatabaseOperations.loadMessagesFor(username);
        if (existingMessages.isEmpty()) {
            synchronized (CONSOLE_LOCK) {
                System.out.println("No messages found.");
            }
        } else {
            for (Message message : existingMessages) {
                printMessage(message);
                lastPrintedMessageId = Math.max(lastPrintedMessageId, message.getId());
            }
        }

        AtomicBoolean running = new AtomicBoolean(true);
        Thread inputThread = new Thread(() -> {
            while (running.get()) {
                final String line;
                try {
                    if (!scanner.hasNextLine()) {
                        running.set(false);
                        break;
                    }
                    line = scanner.nextLine();
                } catch (IllegalStateException e) {
                    running.set(false);
                    break;
                }

                if (line.trim().equalsIgnoreCase("logout")) {
                    running.set(false);
                    synchronized (CONSOLE_LOCK) {
                        System.out.println("Logging out...");
                    }
                    break;
                }

                String[] messageParts = parseMessageInput(line);
                if (messageParts != null) {
                    sendMessage(messageParts[0], messageParts[1], currentUser);
                }
                // All other input is intentionally ignored.
            }
        }, "hush-input");
        inputThread.start();

        // Schedule poll starts one second apart. Polling is done on this thread,
        // while the input thread remains free to wait for the next console line.
        long nextPoll = System.nanoTime() + POLL_INTERVAL_NANOS;
        while (running.get()) {
            long remainingNanos = nextPoll - System.nanoTime();
            if (remainingNanos > 0L) {
                try {
                    long millis = remainingNanos / 1_000_000L;
                    int nanos = (int) (remainingNanos % 1_000_000L);
                    Thread.sleep(millis, nanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running.set(false);
                    break;
                }
            }

            if (!running.get()) {
                break;
            }

            List<Message> newMessages = DatabaseOperations.loadMessagesForAfterId(
                    username, lastPrintedMessageId);
            for (Message message : newMessages) {
                if (message.getId() > lastPrintedMessageId) {
                    printMessage(message);
                    lastPrintedMessageId = message.getId();
                }
            }

            // Avoid rapid back-to-back catch-up polls if a database call took
            // longer than the polling interval.
            nextPoll += POLL_INTERVAL_NANOS;
            if (nextPoll < System.nanoTime()) {
                nextPoll = System.nanoTime() + POLL_INTERVAL_NANOS;
            }
        }

        try {
            inputThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (CONSOLE_LOCK) {
            System.out.println("Farewell!");
        }
    }

    /**
     * Accepts content|recipient or "content"|"recipient". Exactly one pipe is
     * required, and neither field may be empty. Returns null for invalid input.
     */
    private static String[] parseMessageInput(String line) {
        int delimiter = line.indexOf('|');
        if (delimiter <= 0 || delimiter != line.lastIndexOf('|')) {
            return null;
        }

        String content = line.substring(0, delimiter).trim();
        String recipient = line.substring(delimiter + 1).trim();

        content = removeOptionalQuotes(content);
        recipient = removeOptionalQuotes(recipient);
        if (content == null || recipient == null || content.isBlank() || recipient.isBlank()) {
            return null;
        }

        return new String[]{content, recipient};
    }

    /**
     * Returns the unquoted value, or null for mismatched quote characters.
     */
    private static String removeOptionalQuotes(String value) {
        boolean startsWithQuote = value.startsWith("\"");
        boolean endsWithQuote = value.endsWith("\"");

        if (startsWithQuote != endsWithQuote) {
            return null;
        }
        if (startsWithQuote) {
            if (value.length() < 2) {
                return null;
            }
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static void printMessage(Message message) {
        synchronized (CONSOLE_LOCK) {
            System.out.println("----------------------------------------");
            System.out.println("ID: " + message.getId());
            System.out.println("From: " + message.getAuthor());
            System.out.println("Date: " + message.getTimestamp());
            System.out.println("Message: " + message.getContent());
            System.out.println("----------------------------------------");
        }
    }

    /**
     * Sends a message to recipient, using the content and recipient parsed from input.
     */
    private static void sendMessage(String content, String recipient, User currentUser) {
        if (DatabaseOperations.findUser(recipient) == null) {
            synchronized (CONSOLE_LOCK) {
                System.out.println("Error: Recipient '" + recipient + "' does not exist.");
            }
            return;
        }

        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        Message message = new Message(recipient, currentUser.getUsername(), timestamp, content);
        DatabaseOperations.saveMessage(message);

        synchronized (CONSOLE_LOCK) {
            System.out.println("Message sent to " + recipient + ".");
        }
    }
}
