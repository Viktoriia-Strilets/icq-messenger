package nure.ua.client.model;

import java.io.Serializable;

/**
 * Клас для запиту на реєстрацію нового користувача.
 * Містить ім'я користувача та пароль.
 */
public class RegisterRequest implements Serializable {
    private final String username;
    private final String password;

    /**
     * Конструктор для створення запиту на реєстрацію.
     * @param username ім'я користувача
     * @param password пароль користувача
     */
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** @return ім'я користувача */
    public String getUsername() { return username; }

    /** @return пароль користувача */
    public String getPassword() { return password; }
}
