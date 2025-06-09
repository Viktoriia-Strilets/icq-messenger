package nure.ua.client;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Головний клас JavaFX-застосунку.
 * Запускає вікно чату з інтерфейсом користувача.
 */
public class ChatClientApp extends Application {

    /**
     * Точка входу в застосунок.
     * @param args аргументи командного рядка (не використовуються)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Створює та відображає головне вікно застосунку.
     * 
     * @param primaryStage основне вікно
     * @throws IOException у випадку помилки завантаження FXML
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/nure/ua/ChatView.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("ICQ Messenger");
        primaryStage.show();
    }
}
