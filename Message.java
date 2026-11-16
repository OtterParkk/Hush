/** Represents a message within the messaging system. */
public class Message {
    private final long id;
    private final String recipient;
    private final String author;
    private final String timestamp;
    private final String content;

    /** Constructor used when creating a message to be inserted into the database. */
    public Message(String recipient, String author, String timestamp, String content) {
        this(0L, recipient, author, timestamp, content);
    }

    /** Constructor used when reading a message that already has a database ID. */
    public Message(long id, String recipient, String author, String timestamp, String content) {
        this.id = id;
        this.recipient = recipient;
        this.author = author;
        this.timestamp = timestamp;
        this.content = content;
    }

    public long getId() {
        return id;
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
}
