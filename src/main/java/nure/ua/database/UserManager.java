package nure.ua.database;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Session;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Клас, що відповідає за автентифікацію та управління користувачами.
 * Дозволяє реєстрацію, перевірку існування користувача, отримання списку користувачів та їх видалення.
 */
public class UserManager {

    /**
     * Перевіряє правильність логіну та паролю користувача.
     * 
     * @param username ім’я користувача
     * @param password пароль
     * @return true, якщо автентифікація успішна, інакше false
     */   
    public static boolean authenticate(String username, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            UserEntity user = session.get(UserEntity.class, username);
            return user != null && BCrypt.checkpw(password, user.getPasswordHash());
        }
    }

    /**
     * Реєструє нового користувача, якщо ім’я ще не використовується.
     * 
     * @param username ім’я користувача
     * @param password пароль
     * @return true, якщо користувача успішно зареєстровано, інакше false
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static boolean register(String username, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            UserEntity existing = session.get(UserEntity.class, username);
            if (existing != null) return false;

            String hash = BCrypt.hashpw(password, BCrypt.gensalt());

            session.persist(new UserEntity(username, hash, LocalDateTime.now()));

            session.getTransaction().commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }   
    
    }

    /**
     * Перевіряє, чи існує користувач з даним ім’ям.
     * 
     * @param username ім’я користувача
     * @return true, якщо користувач існує
     */
    public static boolean userExists(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            UserEntity user = session.get(UserEntity.class, username);
            return user != null;
        }
    }

    /**
     * Повертає список усіх імен користувачів.
     * 
     * @return список імен користувачів
     */
    public static List<String> getAllUsernames() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
           return session.createQuery("SELECT u.username FROM UserEntity u", String.class)
                         .getResultList();
        }
    }

    /**
     * Видаляє користувача з бази даних.
     * 
     * @param username ім’я користувача
     */
    public static void deleteUser(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            UserEntity user = session.get(UserEntity.class, username);
            if (user != null) session.remove(user);
            session.getTransaction().commit();
        }
    }
}