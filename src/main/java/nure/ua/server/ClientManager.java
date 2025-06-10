package nure.ua.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Session;
import org.hibernate.query.Query;

import nure.ua.common.Message;
import nure.ua.common.MessageType;
import nure.ua.database.HibernateUtil;
import nure.ua.database.MessageEntity;
import nure.ua.database.MessageManager;

/**
 * Менеджер клієнтів, що керує активними підключеннями, повідомленнями та широкомовними подіями.
 */
public class ClientManager {
    private static final Map<String, ObjectOutputStream> clients = new ConcurrentHashMap<>();
    private static MessageManager db;
    private static final List<String> connectionLog = new ArrayList<>();

    /**
     * Ініціалізує менеджер повідомлень.
     */
    public static void initialize() {
        db = new MessageManager();
    }

    /**
     * Перевіряє, чи вже зайняте ім'я користувача серед підключених клієнтів.
     *
     * @param username Ім'я користувача
     * @return true, якщо ім'я вже зайняте
     */
    public static boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    /**
     * Додає нового клієнта до списку активних, надсилає недоставлені повідомлення, оновлює логи.
     *
     * @param username Ім'я користувача
     * @param out Потік для надсилання об'єктів
     * @throws IOException якщо виникла помилка передачі
     */
    public static void addClient(String username, ObjectOutputStream out) throws IOException {
        clients.put(username, out);

        List<MessageEntity> undelivered = db.getUndeliveredMessages(username);
        for (MessageEntity entity : undelivered) {
            Message msg = new Message(entity.getSender(), entity.getReceiver(), entity.getText(), entity.getTimestamp());
            out.writeObject(msg);
            out.flush();
        }
        db.markMessagesAsDelivered(undelivered);

        connectionLog.add("User " + username + " connected at " + LocalDateTime.now());
        broadcastSystemMessage("User " + username + " joined the chat.");
        broadcastUserList();
    }

    /**
     * Видаляє клієнта з активного списку, надсилає оновлений список.
     *
     * @param username Ім'я користувача
     */
    public static void removeClient(String username, boolean isAccountDeleted) {
        clients.remove(username);
        if (isAccountDeleted) {            
            notifyUserDeleted(username);
        } else {            
            broadcastSystemMessage("User " + username + " has disconnected.");
        }

        try {
            broadcastUserList();
        } catch (IOException ignored) {}
    }

    /**
     * Зберігає повідомлення в БД та надсилає його отримувачу (або відправнику, якщо отримувач не в мережі).
     *
     * @param message Повідомлення до збереження та пересилання
     */
    public static void saveAndForwardMessage(MessageEntity message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            message.setDelivered(false);
            message.setType(MessageType.TEXT);
            db.saveMessage(message);

            Message msg = new Message(message.getSender(), message.getReceiver(), message.getText(), message.getTimestamp());

            ObjectOutputStream recipientOut = clients.get(message.getReceiver());
            ObjectOutputStream senderOut = clients.get(message.getSender());

            Set<String> notifiedUsers = new HashSet<>();
            if (recipientOut != null && notifiedUsers.add(message.getReceiver())) {
                recipientOut.writeObject(msg);
                recipientOut.flush();
            } else if (!message.getSender().equals(message.getReceiver()) && senderOut != null && notifiedUsers.add(message.getSender())) {
                senderOut.writeObject(msg);
                senderOut.flush();
            }
        } catch (IOException e) {
            System.err.println("Failed to send message to " + message.getReceiver());
        }
    }

    /**
     * Надсилає системне повідомлення усім підключеним клієнтам.
     *
     * @param text Текст повідомлення
     */
    public static void broadcastSystemMessage(String text) {
        Message msg = new Message("System", "All", text, LocalDateTime.now());
        msg.setType(MessageType.SYSTEM);
        for (ObjectOutputStream out : clients.values()) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Повертає список повідомлень між двома користувачами.
     *
     * @param user1 Перший користувач
     * @param user2 Другий користувач
     * @return Список повідомлень
     */
    public static List<Message> getConversationBetween(String user1, String user2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<MessageEntity> query = session.createQuery(
                "FROM MessageEntity WHERE " +
                "(sender = :u1 AND receiver = :u2) OR " +
                "(sender = :u2 AND receiver = :u1) ORDER BY timestamp", MessageEntity.class
            );
            query.setParameter("u1", user1);
            query.setParameter("u2", user2);
            return query.list().stream().map(e -> new Message(
                e.getSender(), e.getReceiver(), e.getText(), e.getTimestamp()
            )).toList();
        }
    }

    /**
     * Повертає історію повідомлень для одного користувача.
     *
     * @param username Ім'я користувача
     * @return Список повідомлень
     */
    public static List<Message> getMessageHistory(String username) {
        return db.getMessagesForUser(username);
    }

    /**
     * Надсилає всім клієнтам список підключених користувачів.
     *
     * @throws IOException якщо передача не вдалася
     */
    public static void broadcastUserList() throws IOException {
        List<String> users = new ArrayList<>(clients.keySet());
        for (ObjectOutputStream out : clients.values()) {
            try {
                out.writeObject(users);
                out.flush();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Видаляє всі повідомлення, пов’язані з користувачем.
     *
     * @param username Ім'я користувача
     */
    @SuppressWarnings("deprecation")
    public static void deleteMessagesOf(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.createQuery("DELETE FROM MessageEntity m WHERE m.sender = :user OR m.receiver = :user")
                .setParameter("user", username)
                .executeUpdate();
            session.getTransaction().commit();
        }
    }

    /**
     * Повертає список імен активних клієнтів.
     *
     * @return Список імен користувачів
     */
    public static List<String> getUsernames() {
        return new ArrayList<>(clients.keySet());
    }

    /**
     * Широкомовна передача оновленого списку онлайн користувачів.
     */
    public static void broadcastOnlineUsers() {
        try {
            broadcastUserList();
        } catch (IOException e) {
            System.err.println("Failed to broadcast user list.");
        }
    }

    /**
     * Повертає список активних (підключених) користувачів.
     *
     * @return Список користувачів
     */
    public static List<String> getActiveUsers() {
        return new ArrayList<>(clients.keySet());
    }

    /**
     * Повертає лог з'єднань у вигляді рядка.
     *
     * @return Строка з історією підключень
     */
    public static String getConnectionEvents() {
        return String.join("\n", connectionLog);
    }
    
    /**
     * Повертає список усіх зареєстрованих користувачів.
     *
     * @return Список імен користувачів
     */
    public static List<String> getAllKnownUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT u.username FROM UserEntity u", String.class).getResultList();
        }
    }

    /**
     * Повідомляє всіх клієнтів про те, що користувач був видалений.
     *
     * @param username Ім'я видаленого користувача
     */
    public static void notifyUserDeleted(String username) {
        Message msg = new Message("System", "All", "User " + username + " has been deleted", LocalDateTime.now());
        msg.setType(MessageType.DELETE_ACCOUNT_REQUEST);
        for (ObjectOutputStream out : clients.values()) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException ignored) {}
        }
    }

}

