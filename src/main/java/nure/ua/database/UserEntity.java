package nure.ua.database;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Сутність, яка представляє користувача в базі даних.
 * Містить ім'я користувача, хеш паролю та дату останнього входу.
 */
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * Конструктор без параметрів для Hibernate.
     */
    public UserEntity() {}
    
    /**
     * Повний конструктор.
     *
     * @param username ім’я користувача
     * @param passwordHash хеш пароля
     * @param lastLogin час останнього входу
     */
    public UserEntity(String username, String passwordHash,  LocalDateTime lastLogin) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.lastLogin = lastLogin;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

}