package nure.ua.server.service;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

import org.hibernate.Session;

import nure.ua.client.model.LoginRequest;
import nure.ua.database.HibernateUtil;
import nure.ua.database.UserEntity;
import nure.ua.database.UserManager;
import nure.ua.server.ClientManager;

/**
 * Сервіс для обробки запитів на вхід користувачів.
 * Відповідає за авторизацію, реєстрацію та оновлення дати останнього входу.
 */
public class LoginService {

    /**
     * Обробляє запит на вхід або реєстрацію нового користувача.
     *
     * @param login Об'єкт із даними для входу
     * @param out Потік для відправки відповіді клієнту
     * @return true, якщо вхід успішний; false — в іншому випадку
     * @throws IOException при помилці зв'язку
     */
    public static boolean login(LoginRequest login, ObjectOutputStream out) throws IOException {
        if (ClientManager.isUsernameTaken(login.getUsername())) {
            sendResponse(out, "ERROR: Username already taken.");
            return false;
        }

        if (!UserManager.userExists(login.getUsername())) {
            if (!UserManager.register(login.getUsername(), login.getPassword())) {
                sendResponse(out, "ERROR: Failed to register new user.");
                return false;
            }
        } else if (!UserManager.authenticate(login.getUsername(), login.getPassword())) {
            sendResponse(out, "ERROR: Invalid credentials.");
            return false;
        }

        updateLastLogin(login.getUsername());
        ClientManager.addClient(login.getUsername(), out);

        sendResponse(out, ClientManager.getAllKnownUsers());
        ClientManager.broadcastOnlineUsers();

        return true;
    }

    /**
     * Відправляє відповідь клієнту через потік.
     *
     * @param out Потік вихідних даних
     * @param obj Об'єкт для відправки
     * @throws IOException при помилці запису
     */
    private static void sendResponse(ObjectOutputStream out, Object obj) throws IOException {
        out.writeObject(obj);
        out.flush();
    }

    /**
     * Оновлює дату останнього входу користувача в базі даних.
     *
     * @param username Ім'я користувача
     */
    private static void updateLastLogin(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            UserEntity user = session.get(UserEntity.class, username);
            if (user != null) user.setLastLogin(LocalDateTime.now());
            session.getTransaction().commit();
        }
    }
}