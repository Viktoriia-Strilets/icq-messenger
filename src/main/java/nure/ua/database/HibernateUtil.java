package nure.ua.database;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Утилітний клас для створення та управління SessionFactory Hibernate.
 * Відповідає за ініціалізацію Hibernate за конфігураційним файлом.
 */
public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    /**
     * Створює інстанцію SessionFactory, використовуючи конфігураційний файл.
     *
     * @return інстанція SessionFactory
     * @throws ExceptionInInitializerError якщо виникає помилка під час ініціалізації
     */
    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration().configure("nure/ua/hibernate.cfg.xml").buildSessionFactory();
        } catch (HibernateException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Повертає SessionFactory для створення сесій.
     *
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Закриває поточну SessionFactory.
     */
    public static void shutdown() {
        getSessionFactory().close();
    }
}
