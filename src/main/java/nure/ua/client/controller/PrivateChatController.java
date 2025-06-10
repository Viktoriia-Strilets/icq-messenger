package nure.ua.client.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import nure.ua.client.service.ClientService;
import nure.ua.common.Message;
import nure.ua.common.MessageType;

/**
 * Контролер для вкладки приватного чату між двома користувачами.
 * Забезпечує надсилання та прийом повідомлень, а також відображення історії чату.
 */
public class PrivateChatController {

    @FXML private TextArea chatArea;       // Область для виведення повідомлень
    @FXML private TextField inputField;    // Поле для введення тексту повідомлення

    private String myUsername;             // Ім’я поточного користувача
    private String peerUsername;           // Ім’я співрозмовника
    private ClientService client;          // Сервіс клієнта для взаємодії з сервером

    private final Set<String> displayedMessages = new HashSet<>(); // Унікальні ідентифікатори отриманих повідомлень для запобігання дублювання
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Формат дати і часу

    /**
     * Ініціалізує приватний чат з переданими даними.
     * Завантажує історію повідомлень з обраним користувачем.
     *
     * @param client сервіс для взаємодії з сервером
     * @param myUsername ім’я поточного користувача
     * @param peerUsername ім’я користувача, з яким ведеться чат
     */
    public void initializeData(ClientService client, String myUsername, String peerUsername) {
        this.client = client;
        this.myUsername = myUsername;
        this.peerUsername = peerUsername;

        if (!peerUsername.equals(this.peerUsername)) {
            chatArea.clear();
            displayedMessages.clear();
            appendMessage("Chat with " + peerUsername + " opened.");
        }       

        
        try {
            client.requestHistoryWith(peerUsername);
        } catch (IOException e) {
            appendMessage("Failed to load history.");
        }
    }

    /**
     * Отримує повідомлення (історію або нове) та додає його до чату,
     * уникаючи повторного відображення вже отриманих повідомлень.
     *
     * @param msg повідомлення, яке потрібно обробити
     */
    public void receiveMessage(Message msg) {
        String timeKey = msg.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String uniqueId = msg.getSender() + "|" + timeKey + "|" + msg.getText();
        if (displayedMessages.contains(uniqueId)) {
            return; 
        }
        displayedMessages.add(uniqueId);

        String time = msg.getTimestamp().format(TIME_FORMATTER);
        String sender = msg.getSender();
        String text = msg.getText();
        MessageType mt = msg.getType();
        String displaySender = sender.equals(myUsername) ? "ME" : sender;

        switch (mt) {
            case HISTORY_RESPONSE, TEXT -> appendMessage("[" + time + "]\t" + displaySender + ": " + text + "\n");
            case SYSTEM -> appendMessage("[System]: " + text + "    [" + time + "]");
            default -> appendMessage("[" + time + "]\t" + sender + ": " + text + "\n");
        }
    }

    /**
     * Додає повідомлення до вікна чату.
     *
     * @param text текст для відображення
     */
    private void appendMessage(String text) {
        chatArea.appendText(text + "\n");
    }

    /**
     * Надсилає нове повідомлення на сервер та додає його до чату локально.
     * Викликається при натисканні кнопки "Надіслати" або клавіші Enter.
     */
    @FXML
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            try {
                client.sendMessage(myUsername, peerUsername, text);
                Message localMsg = new Message(myUsername, peerUsername, text, LocalDateTime.now());
                localMsg.setType(MessageType.TEXT);
                receiveMessage(localMsg);
                inputField.clear();
            } catch (IOException e) {
                appendMessage("Failed to send message.");
            }
        }
    }

}