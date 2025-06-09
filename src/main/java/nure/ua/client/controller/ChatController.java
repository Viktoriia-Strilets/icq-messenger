package nure.ua.client.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import nure.ua.client.service.ClientService;
import nure.ua.common.Message;
import nure.ua.common.MessageType;
import nure.ua.server.ClientManager;

/**
 * Контролер головного вікна чату.
 * Відповідає за управління UI, підключенням/відключенням від сервера,
 * відкриттям приватних чатів, обробкою списку користувачів і повідомлень.
 */
public class ChatController {
    @FXML private TextArea messagesArea;           // Загальна область для виводу повідомлень
    @FXML private ListView<String> usersList;      // Список відомих користувачів
    @FXML private TextField usernameInput;          // Поле для введення імені користувача
    @FXML private TextField passwordInput;          // Поле для введення пароля
    @FXML private Button connectButton;              // Кнопка підключення до сервера
    @FXML private Button disconnectButton;           // Кнопка відключення від сервера
    @FXML private TextArea systemLogArea;            // Область для системних логів
    @FXML private TabPane privateChatsTabPane;        // Панель вкладок для приватних чатів
    @FXML private Button deleteAccountButton;        // Кнопка видалення акаунту

    private ClientService client;                      // Сервіс клієнта для роботи з сервером
    private String myUsername;                         // Ім’я поточного користувача
    private final Map<String, Tab> openedChatTabs = new HashMap<>();   // Відкриті вкладки приватних чатів
    private final Map<String, Stage> privateChats = new HashMap<>();   // Вікна приватних чатів (неактивне)
    private final Map<String, PrivateChatController> chatControllers = new HashMap<>();  // Контролери приватних чатів

    private final List<String> knownUsers = new ArrayList<>();    // Відомі користувачі (онлайн і офлайн)
    private final List<String> onlineUsers = new ArrayList<>();   // Користувачі, які зараз онлайн

    /**
     * Ініціалізація GUI-компонентів і налаштування обробників подій.
     * Викликається після завантаження FXML.
     */
    @FXML
    public void initialize() {
         // Ініціалізує сервіс клієнта, встановлює обробники кнопок і списку користувачів
        client = new ClientService();

        connectButton.setOnAction(e -> connect());
        disconnectButton.setOnAction(e -> disconnect());
        disconnectButton.setDisable(true);
        deleteAccountButton.setOnAction(e -> deleteAccount());
        usersList.setOnMouseClicked(event -> {
            String selected = usersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String peerUsername = extractPureUsername(selected);

                if (!peerUsername.equals(usernameInput.getText().trim())) {
                    openPrivateChat(peerUsername);
                    try {
                        client.requestHistoryWith(peerUsername);
                    } catch (IOException ex) {
                        showAlert("Error", "Failed to retrieve message history: " + ex.getMessage());
                    }
                } else {
                    messagesArea.clear();
                    messagesArea.appendText("System log:\n");
                    messagesArea.appendText(ClientManager.getConnectionEvents());
                }
            }
        });
    }
    
    /**
     * Підключення до серверу з логіном і паролем, введеними в UI.
     * Виводить помилки в разі невдалого підключення.
     */
    private void connect() {
        String username = usernameInput.getText().trim();
        String password = passwordInput.getText().trim();
        myUsername = username;
        if (username.isEmpty()) {
            showAlert("Username required", "Please enter your username before connecting.");
            return;
        }       
            
        try {
            client.start(username, password, this::onMessageReceived, this::onUsersListReceived);
            if (!client.getInitialResponse().isEmpty()) {
                showAlert("Connection Error", client.getInitialResponse());
                client.close();
                client = new ClientService();
                return;
            }
            appendMessage("Connected as " + username);
            connectButton.setDisable(true);
            usernameInput.setDisable(true);
            passwordInput.setDisable(true);
            disconnectButton.setDisable(false);
            deleteAccountButton.setDisable(false);
        } catch (IOException ex) {
            showAlert("Connection failed", "Could not connect to server: " + ex.getMessage());
        }
    }
    
    /**
     * Обробник отримання повідомлень від сервера.
     * Розподіляє повідомлення за типом і оновлює відповідні UI-компоненти.
     * @param msg повідомлення, отримане від сервера
     */
    @SuppressWarnings("UnnecessaryReturnStatement")
    private void onMessageReceived(Message msg) {
            Platform.runLater(() -> {
                if (msg.getType() == MessageType.SYSTEM && msg.getText().startsWith("User ") && msg.getText().endsWith("has been deleted")) {
                    String deletedUser = msg.getText().substring(5, msg.getText().indexOf(" has been deleted")).trim();
                    knownUsers.remove(deletedUser);
                    onlineUsers.remove(deletedUser);

                    if (openedChatTabs.containsKey(deletedUser)) {
                        Tab tab = openedChatTabs.get(deletedUser);
                        privateChatsTabPane.getTabs().remove(tab);
                        openedChatTabs.remove(deletedUser);
                        chatControllers.remove(deletedUser);
                    }

                    appendSystemLog("User " + deletedUser + " has been deleted.");
                    updateUsersList();
                    return;
                }  

                if (msg.getType() == MessageType.SYSTEM) {
                    appendSystemLog("[" + msg.getTimestamp() + "] " + msg.getText());
                    return;
                }

                String currentUser = usernameInput.getText().trim();
                String peer = msg.getSender().equals(currentUser) ? msg.getReceiver() : msg.getSender();

                if (!privateChats.containsKey(peer)) {
                    openPrivateChat(peer);
                }

                PrivateChatController controller = chatControllers.get(peer);
                if (controller != null) {
                    controller.receiveMessage(msg);
                } else {
                    System.err.println("Controller for " + peer + " not found.");
                }
              
            });
    }

    /**
     * Оновлення списку користувачів, які онлайн та офлайн.
     * Відображає статус поруч з іменем користувача.
     * @param users список імен користувачів, які зараз онлайн
     */
    private void onUsersListReceived(List<String> users) {
        Platform.runLater(() -> {
            for (String user : users) {
               if (!knownUsers.contains(user)) {
                   knownUsers.add(user);
                }
            }
                onlineUsers.clear();
                onlineUsers.addAll(users);

                usersList.getItems().clear();
                for (String user : knownUsers) {
                    if (user.equals(myUsername)) continue;
                    boolean isOnline = onlineUsers.contains(user);
                    String display = user + (isOnline ? " (online)" : " (offline)");
                    usersList.getItems().add(display);
                }

                for (Map.Entry<String, Tab> entry : openedChatTabs.entrySet()) {
                    String peerUsername = entry.getKey();
                    Tab tab = entry.getValue();
                    boolean isOnline = onlineUsers.contains(peerUsername);
                    String newTabName = peerUsername + (isOnline ? " (online)" : " (offline)");
                    tab.setText(newTabName);
                }
            
        });
    }

    /**
     * Додає текст у загальну область повідомлень.
     * @param text текст повідомлення
     */
    private void appendMessage(String text) {
        messagesArea.appendText(text + "\n");
    }

    /**
     * Додає текст у область системних логів.
     * @param text текст системного повідомлення
     */
    private void appendSystemLog(String text) {
        Platform.runLater(() -> {
            if (systemLogArea != null) {
                systemLogArea.appendText(text + "\n");
            }
        });
    }
 
    /**
     * Оновлення UI списку користувачів та вкладок приватних чатів при зміні статусу.
     */
    private void updateUsersList() {
        Platform.runLater(() -> {
            usersList.getItems().clear();
            for (String user : knownUsers) {
                if (user.equals(myUsername)) continue;
                boolean isOnline = onlineUsers.contains(user);
                String display = user + (isOnline ? " (online)" : " (offline)");
                usersList.getItems().add(display);
            }

            for (Map.Entry<String, Tab> entry : openedChatTabs.entrySet()) {
                String peerUsername = entry.getKey();
                Tab tab = entry.getValue();
                boolean isOnline = onlineUsers.contains(peerUsername);
                String newTabName = peerUsername + (isOnline ? " (online)" : " (offline)");
                tab.setText(newTabName);
            }
        });
    }
    
    /**
     * Відключення від сервера, очищення UI та відновлення стану кнопок.
     */
    private void disconnect() {
        client.close();
        appendMessage("Disconnected from server.");        
        disconnectButton.setDisable(true);
        connectButton.setDisable(false);
        usernameInput.setDisable(false);
        passwordInput.setDisable(false);
        deleteAccountButton.setDisable(true);
        myUsername = null;
    }
   
    /**
     * Надсилає запит на видалення облікового запису користувача.
     * Після успіху очищує UI та показує повідомлення.
     */
    private void deleteAccount() {
        if (client != null && myUsername != null) {
            try {
                client.deleteAccount();
                appendMessage("Your account has been deleted.");
                disconnect();
                resetUI();
                showAlert("Account Deleted", "Your account and messages have been permanently removed.");
            } catch (IOException ex) {
                showAlert("Error", "Failed to delete account: " + ex.getMessage());
            }
        }
    }

    /**
     * Відкриває вкладку приватного чату для спілкування з певним користувачем.
     * Якщо вкладка вже існує, активує її.
     * @param peerUsername ім'я користувача для приватного чату
     */
    private void openPrivateChat(String peerUsername) {

        if (openedChatTabs.containsKey(peerUsername)) {
            Tab tab = openedChatTabs.get(peerUsername);
            privateChatsTabPane.getSelectionModel().select(tab);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/nure/ua/PrivateChatWindow.fxml"));
            Parent root = loader.load();
            PrivateChatController controller = loader.getController();
            controller.initializeData(client, myUsername, peerUsername);

            boolean isOnline = onlineUsers.contains(peerUsername);
            String tabName = peerUsername + (isOnline ? " (online)" : " (offline)");
            Tab tab = new Tab(tabName);

            openedChatTabs.put(peerUsername, tab);

            tab.setContent(root);
            tab.setOnClosed(e -> {
                chatControllers.remove(peerUsername);
                openedChatTabs.remove(peerUsername);
            });

            privateChatsTabPane.getTabs().add(tab);
            privateChatsTabPane.getSelectionModel().select(tab);

            chatControllers.put(peerUsername, controller);

        } catch (IOException e) {
        showAlert("Error", "Could not open chat tab: " + e.getMessage());
        }
    }

    /**
     * Допоміжний метод для вилучення імені користувача без статусу (online/offline).
     * @param displayedName рядок з іменем та статусом
     * @return чисте ім'я користувача без статусу
     */
    private String extractPureUsername(String displayName) {
        return displayName.replace(" (online)", "").replace(" (offline)", "").trim();
    }

    /**
     * Показує діалогове вікно з повідомленням.
     * @param title заголовок діалогу
     * @param message текст повідомлення
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Очищення UI після видалення акаунту або відключення.
     */
    private void resetUI() {
        usernameInput.clear();
        passwordInput.clear();

        usersList.getItems().clear();
        knownUsers.clear();
        onlineUsers.clear();

        privateChatsTabPane.getTabs().clear();
        openedChatTabs.clear();
        privateChats.clear();
        chatControllers.clear();

        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        usernameInput.setDisable(false);
        passwordInput.setDisable(false);
    }
    
    /** @return всіх користувачів */
    private List<String> getAllKnownUsers() {
       return new ArrayList<>(knownUsers);
    }

        /** @return приватні чати між користувачами */
    public Map<String, Stage> getPrivateChats() {
        return privateChats;
    }
}