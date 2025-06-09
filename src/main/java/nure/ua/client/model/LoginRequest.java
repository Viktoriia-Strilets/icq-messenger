package nure.ua.client.model;

import java.io.Serializable;

/**
 * Клас для запиту на логін або реєстрацію користувача.
 * Містить ім'я користувача, пароль та інформацію, чи це запит на реєстрацію.
 */
public class LoginRequest implements Serializable {
    private final String username;
    private final String password;
    private boolean isRegistration;

    /**
     * Конструктор для створення запиту на логін або реєстрацію.
     * 
     * @param username ім'я користувача
     * @param password пароль користувача
     * @param isRegistration true, якщо це запит на реєстрацію, інакше false
     */
    public LoginRequest(String username, String password, boolean isRegistration) {
        this.username = username;
        this.password = password;
        this.isRegistration = isRegistration;
    }

    /** @return ім'я користувача */
    public String getUsername() { return username; }

    /** @return пароль користувача */
    public String getPassword() { return password; }

    /** @return true, якщо це реєстрація, інакше false */
    public boolean isRegistration() { return isRegistration; }

    /**
     * Встановлює статус запиту як реєстрації або логіну.
     * @param isRegistration true для реєстрації, false для логіну
     */
    public void setIsRegistration(boolean isRegistration) {
        this.isRegistration = isRegistration;
    }
}