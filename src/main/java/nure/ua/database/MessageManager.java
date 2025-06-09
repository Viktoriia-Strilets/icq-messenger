package nure.ua.database;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import nure.ua.common.Message;
import nure.ua.common.MessageType;

/**
 * Клас, який відповідає за збереження, отримання та оновлення повідомлень у базі даних.
 * Використовує Hibernate для взаємодії з таблицею повідомлень.
 */
public class MessageManager {

    public MessageManager() {}

    /**
     * Зберігає повідомлення у базі даних.
     *
     * @param msg екземпляр повідомлення
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void saveMessage(MessageEntity msg) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            msg.setDelivered(false);
            msg.setType(MessageType.TEXT);
            session.persist(msg);
            session.getTransaction().commit();
        } catch (Exception e) {
            System.err.println("Error saving message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Отримує список недоставлених повідомлень для заданого користувача.
     *
     * @param username ім’я користувача
     * @return список недоставлених повідомлень
     */
    public List<MessageEntity> getUndeliveredMessages(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<MessageEntity> query = session.createQuery(
                "FROM MessageEntity WHERE receiver = :username AND delivered = false", MessageEntity.class);
            query.setParameter("username", username);
            return query.list();
        }
    }

    /**
     * Відмічає повідомлення як доставлені.
     *
     * @param messages список повідомлень
     */    
    public void markMessagesAsDelivered(List<MessageEntity> messages) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            for (MessageEntity msg : messages) {
                MessageEntity m = session.find(MessageEntity.class, msg.getId());
                if (m != null) {
                    m.setDelivered(true);
                }
            }
            tx.commit();
        }
    }

    /**
     * Отримує історію повідомлень користувача.
     *
     * @param username ім’я користувача
     * @return список повідомлень
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public List<Message> getMessagesForUser(String username) {
        List<Message> messages = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MessageEntity WHERE sender = :username OR recipient = :username ORDER BY timestamp";
            Query<MessageEntity> query = session.createQuery(hql, MessageEntity.class);
            query.setParameter("username", username);
            List<MessageEntity> results = query.list();
            for (MessageEntity entity : results) {
                messages.add(new Message(
                        entity.getSender(),
                        entity.getReceiver(),
                        entity.getText(),
                        entity.getTimestamp()
                ));

            }        

        } catch (Exception e) {
            System.err.println("Error fetching message history: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Отримує список повідомлень між двома користувачами.
     *
     * @param user1 перший користувач
     * @param user2 другий користувач
     * @return список повідомлень
     */
    public List<Message> getConversationBetween(String user1, String user2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM MessageEntity WHERE " +
                         " (sender = :u1 AND receiver = :u2) OR (sender = :u2 AND receiver = :u1) " +
                         "ORDER BY timestamp";                    
            Query<MessageEntity> query = session.createQuery(hql, MessageEntity.class);
            query.setParameter("u1", user1);
            query.setParameter("u2", user2);
            return query.list().stream().map(e -> new Message(
                    e.getSender(), e.getReceiver(), e.getText(), e.getTimestamp()
            )).toList();
        }
    }

}
