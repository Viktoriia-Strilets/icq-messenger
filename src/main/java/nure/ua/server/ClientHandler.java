package nure.ua.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import nure.ua.client.model.LoginRequest;
import nure.ua.client.model.RegisterRequest;
import nure.ua.common.Message;
import nure.ua.database.UserManager;
import nure.ua.server.service.LoginService;
import nure.ua.server.service.MessageProcessor;

/**
 * Потік, який обробляє окреме з’єднання клієнта.
 * Підключається, автентифікує користувача і передає повідомлення до відповідного сервісу.
 */
public class ClientHandler extends Thread {
    private final Socket socket;
    private String username;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /**
     * Конструктор для створення обробника клієнта.
     *
     * @param socket Сокет підключення до клієнта
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Основний цикл обробки клієнта: автентифікація та обробка повідомлень.
     */
    @Override
    public void run() {
        try {
            setupStreams();

            Object inputObj = in.readObject();
            if (!processInitialRequest(inputObj)) return;

            MessageProcessor processor = new MessageProcessor(username, out);

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Message msg) {
                    processor.process(msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + username);
        } finally {
            disconnectClient();
        }
    }

    /**
     * Ініціалізує потоки вводу/виводу з клієнтом.
     *
     * @throws IOException при помилках відкриття потоків
     */
    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * Обробляє перший запит від клієнта: вхід або реєстрація.
     *
     * @param inputObj Об'єкт, отриманий від клієнта
     * @return true, якщо вхід успішний; false — інакше
     * @throws IOException при помилках зв'язку
     */
    private boolean processInitialRequest(Object inputObj) throws IOException {
        switch (inputObj) {
            case LoginRequest login -> {
                boolean success = LoginService.login(login, out);
                if (success) {
                    this.username = login.getUsername();  
                }
                return success;
            }       case RegisterRequest reg -> {
                boolean success = UserManager.register(reg.getUsername(), reg.getPassword());
                out.writeObject(success ? "OK: Registration successful." : "ERROR: Username already exists.");
                out.flush();
                return false;
            }       default -> {
                out.writeObject("ERROR: Expected login request.");
                out.flush();
                return false;
            }  
        }
    }

    /**
     * Завершує підключення клієнта та очищає ресурси.
     */
    private void disconnectClient() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }    

}

