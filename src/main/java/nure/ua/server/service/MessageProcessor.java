package nure.ua.server.service;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.List;

import nure.ua.common.Message;
import nure.ua.common.MessageType;
import nure.ua.database.MessageEntity;
import nure.ua.database.UserManager;
import nure.ua.server.ClientManager;

/**
 * Клас, який обробляє повідомлення, отримані від клієнта.
 * Підтримує текстові повідомлення, історію переписок та запити на видалення акаунта.
 */
public class MessageProcessor {
    private final String username;
    private final ObjectOutputStream out;

    /**
     * Конструктор, що ініціалізує процесор для конкретного користувача.
     *
     * @param username Ім’я користувача
     * @param out Потік для відправки відповідей клієнту
     */
    public MessageProcessor(String username, ObjectOutputStream out) {
        this.username = username;
        this.out = out;
    }

    /**
     * Обробляє отримане повідомлення в залежності від його типу.
     *
     * @param msg Повідомлення для обробки
     * @throws IOException при помилках зв'язку
     */
    public void process(Message msg) throws IOException {
        switch (msg.getType()) {
            case TEXT -> handleTextMessage(msg);
            case HISTORY_REQUEST -> handleHistoryRequest(msg);
            case DELETE_ACCOUNT_REQUEST -> handleAccountDeletion(msg);
            case DISCONNECT_NOTIFICATION -> handleDisconnect(msg);
            default -> System.out.println("Unknown message type from user: " + username);
        }
    }

    /**
     * Обробляє текстове повідомлення — зберігає його та перенаправляє.
     *
     * @param msg Текстове повідомлення
     * @throws IOException при помилках відправки
     */
    private void handleTextMessage(Message msg) throws IOException {
        ClientManager.saveAndForwardMessage(new MessageEntity(
            msg.getSender(), msg.getReceiver(), msg.getText(), msg.getTimestamp(), MessageType.TEXT, false
        ));
    }

    /**
     * Обробляє запит на історію повідомлень між користувачами.
     *
     * @param msg Запит на історію
     * @throws IOException при помилках відправки
     */
    private void handleHistoryRequest(Message msg) throws IOException {
        List<Message> history = ClientManager.getConversationBetween(msg.getSender(), msg.getReceiver());
        for (Message message : history) {
            Message historyMsg = new Message(
                message.getSender(), message.getReceiver(), message.getText(), message.getTimestamp());
            historyMsg.setType(MessageType.HISTORY_RESPONSE);
            out.writeObject(historyMsg);
            out.flush();
        }
    }

    /**
     * Обробляє запит на видалення акаунта: видаляє повідомлення, акаунт, надсилає підтвердження.
     *
     * @param msg Запит на видалення акаунта
     * @throws IOException при помилках зв'язку
     */
    private void handleAccountDeletion(Message msg) throws IOException {
        String userToDelete = msg.getSender();
        Message confirmation = new Message("Server", userToDelete, "Account deleted", LocalDateTime.now());
        confirmation.setType(MessageType.DELETE_ACCOUNT_CONFIRMATION);
        out.writeObject(confirmation);
        out.flush();

        UserManager.deleteUser(userToDelete);
        ClientManager.deleteMessagesOf(userToDelete); 
        ClientManager.removeClient(userToDelete, true);
        
        ClientManager.broadcastOnlineUsers();
    }

    /**
     * Обробляє запит на від'єднання акаунта.
     *
     * @param msg Запит на від'єднання акаунта
     * @throws IOException при помилках зв'язку
     */
    private void handleDisconnect(Message msg) throws IOException {
        String userToDisconnect = msg.getSender();

        System.out.println("User disconnected: " + userToDisconnect);

        ClientManager.removeClient(userToDisconnect, false); 
        ClientManager.broadcastOnlineUsers();
    }
}