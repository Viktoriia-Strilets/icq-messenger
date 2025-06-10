package nure.ua.client.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import nure.ua.client.ChatClient;
import nure.ua.common.Message;
import nure.ua.common.MessageType;

/**
 * Сервіс для роботи з клієнтом чат-застосунку.
 * Забезпечує підключення до сервера, відправку та прийом повідомлень,
 * а також запити на історію та видалення акаунту.
 */
public class ClientService {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private String initialResponse = "";

    private ChatClient chatClient;

    /**
     * Запускає з'єднання з сервером, автентифікує користувача.
     * Після успішного підключення починає слухати вхідні повідомлення і оновлення списку користувачів.
     * 
     * @param username ім'я користувача
     * @param password пароль користувача
     * @param onMessage колбек для отримання нових повідомлень
     * @param onUsers колбек для оновлення списку користувачів
     * @throws IOException у випадку помилок підключення
     */
    public void start(String username, String password,
                            Consumer<Message> onMessage,
                            Consumer<List<String>> onUsers) throws IOException {

        this.username = username;
        this.chatClient = new ChatClient();

        boolean success = chatClient.connect(username, password);
        this.initialResponse = chatClient.getInitialResponse();
        if (!success) {
            return;
        }        

        this.out = chatClient.getOut();
        this.in = chatClient.getIn();

        out.flush();

        try {
            Object response = in.readObject();
            if (response instanceof String str && str.startsWith("ERROR:")) {
                initialResponse = str;
                return;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Invalid response from server", e);
        }

         chatClient.listen(onMessage, onUsers);
    }

    /**
     * Надсилає текстове повідомлення від користувача до іншого користувача.
     * 
     * @param from відправник
     * @param to отримувач
     * @param text текст повідомлення
     * @throws IOException у випадку проблем з мережею
     */
    public void sendMessage(String from, String to, String text) throws IOException {
        Message msg = new Message(from, to, text, LocalDateTime.now());
        out.writeObject(msg);
        out.flush();
    }
    
    /**
     * Запитує історію повідомлень з певним користувачем.
     * 
     * @param peerUsername ім'я користувача, з яким запитуємо історію
     * @throws IOException у випадку проблем з мережею
     */
    public void requestHistoryWith(String peerUsername) throws IOException {
        Message request = new Message(username, peerUsername, "", LocalDateTime.now());
        request.setType(MessageType.HISTORY_REQUEST);
        out.writeObject(request);
        out.flush();
    }


    /**
     * Закриває з'єднання та звільняє ресурси.
     */
    public void close() {
    try {
        if (out != null) {
            Message disconnectMsg = new Message(username, null, "User disconnected", LocalDateTime.now());
            disconnectMsg.setType(MessageType.DISCONNECT_NOTIFICATION);
            out.writeObject(disconnectMsg);
            out.flush();
        }
    } catch (IOException e) {
        System.err.println("Failed to send disconnect notification: " + e.getMessage());
    }
    if (chatClient != null) {
        chatClient.close();
    }
}

    /**
     * Відправляє запит на видалення акаунту користувача.
     * 
     * @throws IOException у випадку проблем з мережею
     */
    public void deleteAccount() throws IOException {
        Message deleteRequest = new Message(username, null, "", LocalDateTime.now());
        deleteRequest.setType(MessageType.DELETE_ACCOUNT_REQUEST);
        out.writeObject(deleteRequest);
        out.flush();    
    }

    /** @return початкову відповідь сервера після підключення */
    public String getInitialResponse() {
        return initialResponse;
    }

    /** @return ім'я користувача */
    public String getUsername() {
        return username;
    }

    /** @return список усіх відомих користувачів */
    public List<String> getAllKnownUsers() {
       return chatClient != null ? chatClient.getAllKnownUsers() : List.of();
    }
}