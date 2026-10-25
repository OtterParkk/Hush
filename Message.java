public class Message {
    private final String recipient;
    private final String author;
    private final String timestamp;
    private final String content;

    public Message(String recipient, String author, String timestamp, String content) {
        this.recipient = recipient;
        this.author = author;
        this.timestamp = timestamp;
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getAuthor() {
        return author;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        return content;
    }

    public String toFileString() {
        return recipient + "|" + author + "|" + timestamp + "|" + content;
    }

    public static Message fromFileString(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length == 4) {
            return new Message(parts[0], parts[1], parts[2], parts[3]);
        }
        return null;
    }
}