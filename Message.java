/**
* Represents a message within the messaging system
* Useful for data organization and encapsulation
*/

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
}
