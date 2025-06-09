package nure.ua.database;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import nure.ua.common.MessageType;

/**
 * Сутність, що представляє повідомлення в базі даних.
 * Містить інформацію про відправника, отримувача, текст, час відправлення, тип та статус доставки.
 */
@Entity
@Table(name = "messages")
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    

    private String sender;
    private String receiver;
    @Column(name = "message")
    private String text;
    private LocalDateTime timestamp;
    private boolean delivered = false;
    @Enumerated(EnumType.STRING)
    private MessageType type;

    /**
     * Конструктор без параметрів для Hibernate.
     */
    public MessageEntity() {}

    /**
     * Повний конструктор.
     */
    public MessageEntity(String sender, String receiver, String text, LocalDateTime timestamp, MessageType type, boolean delivered) {
        this.sender = sender;
        this.receiver = receiver;
        this.text = text;
        this.timestamp = timestamp;
        this.type = type;
        this.delivered = delivered;
    }

    public Long getId() {  return id;  }
    public void setId(Long id) { this.id = id; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
}

