package nure.ua.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import nure.ua.client.model.LoginRequest;
import nure.ua.common.Message;
import nure.ua.common.MessageType;

/**
 * Клас для керування з'єднанням з сервером чату.
 * Відповідає за встановлення сокет-з'єднання, автентифікацію,
 * надсилання та отримання повідомлень.
 */
public class ChatClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private Thread listenerThread;

    private String initialResponse = "";
    private List<String> knownUsers = List.of();
    private boolean allUsersReceived = false; 

    /**
     * Підключається до сервера та виконує автентифікацію користувача.
     * 
     * @param username ім'я користувача
     * @param password пароль користувача
     * @return true, якщо підключення та автентифікація успішні, інакше false
     */
    public boolean connect(String username, String password) {
        
        try {
            this.username = username;
            socket = new Socket("localhost", 8000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new LoginRequest(username, password, false));
            out.flush();

            Object response = in.readObject();
            if (response instanceof String str && str.startsWith("ERROR:")) {
                initialResponse = str;
                return false;
            }
            return true;

        } catch (IOException | ClassNotFoundException e) {
            initialResponse = "Connection error: " + e.getMessage();
            return false;
        }
    }

    /**
     * Запускає окремий потік для прослуховування вхідних повідомлень
     * і оновлення списку користувачів.
     * 
     * @param onMessage колбек для обробки отриманих повідомлень
     * @param onUsers колбек для оновлення списку користувачів
     */
    public void listen(Consumer<Message> onMessage, Consumer<List<String>> onUsers) {
        listenerThread = new Thread(() -> {
            try {
                while (true) {
                    Object input = in.readObject();
                    if (input instanceof Message msg) {
                        Platform.runLater(() -> onMessage.accept(msg));
                        if (msg.getType() == MessageType.DELETE_ACCOUNT_CONFIRMATION) {
                            Platform.runLater(() -> {
                                System.out.println("Account deletion confirmed.");
                            });
                        }
                    } else if (input instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String) {
                        @SuppressWarnings("unchecked")
                        List<String> users = (List<String>) list;
                        if (!allUsersReceived) {
                            knownUsers = users;
                            Platform.runLater(() -> onUsers.accept(users));
                            allUsersReceived = true;
                        } else {
                            Platform.runLater(() -> onUsers.accept(users));
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Listener error: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Надсилає текстове повідомлення до сервера.
     * 
     * @param from відправник
     * @param to отримувач
     * @param text текст повідомлення
     * @throws IOException у випадку проблем з мережею
     */
    public void sendMessage(String from, String to, String text) throws IOException {
        Message msg = new Message(from, to, text, LocalDateTime.now());
        msg.setType(MessageType.TEXT);
        out.writeObject(msg);
        out.flush();
    }

    /**
     * Закриває всі відкриті ресурси та припиняє з'єднання.
     */
    public void close() {
        try {
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Failed to close connection: " + e.getMessage());
        }
    }

    /** @return початкову відповідь сервера після підключення */
    public String getInitialResponse() {
        return initialResponse;
    }

    /** @return список відомих користувачів */
    public List<String> getAllKnownUsers() {
        return new ArrayList<>(knownUsers);
    }  

    /** @return об'єкт ObjectOutputStream для відправки повідомлень */
    public ObjectOutputStream getOut() {
       return out;
    }

    /** @return об'єкт ObjectInputStream для отримання повідомлень */
    public ObjectInputStream getIn() {
       return in;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

