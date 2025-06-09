package nure.ua.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Клас, що представляє повідомлення, яке передається між користувачами.
 * Містить інформацію про відправника, отримувача, текст повідомлення,
 * час надсилання та тип повідомлення.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sender;                  // Ім’я користувача, який надіслав повідомлення
    private String receiver;                // Ім’я користувача, якому призначене повідомлення
    private String text;                    // Текст повідомлення
    private LocalDateTime timestamp;        // Час надсилання повідомлення
    private MessageType type;               // Тип повідомлення (наприклад, текстове, системне тощо)

    /**
     * Конструктор для створення текстового повідомлення.
     *
     * @param sender відправник
     * @param receiver отримувач
     * @param text вміст повідомлення
     * @param timestamp час створення повідомлення
     */
    public Message(String sender, String receiver, String text, LocalDateTime timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
        this.timestamp = timestamp;
        this.type = MessageType.TEXT;
    }

    /**
     * @return ім’я відправника повідомлення
     */
    public String getSender() { return sender; }

    /**
     * Встановлює ім’я відправника повідомлення.
     * @param sender ім’я відправника
     */
    public void setSender(String sender) { this.sender = sender; }

    /**
     * @return ім’я отримувача повідомлення
     */
    public String getReceiver() { return receiver; }

    /**
     * Встановлює ім’я отримувача повідомлення.
     * @param receiver ім’я отримувача
     */
    public void setReceiver(String receiver) { this.receiver = receiver; }

    /**
     * @return текст повідомлення
     */
    public String getText() { return text; }

    /**
     * Встановлює текст повідомлення.
     * @param text вміст повідомлення
     */
    public void setText(String text) { this.text = text; }

    /**
     * @return дата та час надсилання повідомлення
     */
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * Встановлює дату та час повідомлення.
     * @param timestamp дата і час
     */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    /**
     * @return тип повідомлення
     */
    public MessageType getType() { return type; }

    /**
     * Встановлює тип повідомлення.
     * @param type тип (TEXT, SYSTEM тощо)
     */
    public void setType(MessageType type) { this.type = type; }

    /**
     * @return рядкове представлення об’єкта Message
     */
    @Override
    public String toString() {
        return "Message{" +
               "from='" + sender + '\'' +
               ", to='" + receiver + '\'' +
               ", text='" + text + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}

