package nure.ua.common;

/**
 * Перелік типів повідомлень, які можуть використовуватись у чаті або
 * системній взаємодії.
 */
public enum MessageType {
    /**
     * Стандартне текстове повідомлення між користувачами.
     */
    TEXT,

    /**
     * Запит на історію повідомлень з певним користувачем.
     */
    HISTORY_REQUEST,

    /**
     * Відповідь на запит історії повідомлень.
     */
    HISTORY_RESPONSE,

    /**
     * Запит на видалення облікового запису.
     */
    DELETE_ACCOUNT_REQUEST,

    /**
     * Підтвердження про успішне видалення облікового запису.
     */
    DELETE_ACCOUNT_CONFIRMATION,

    /**
     * Системне повідомлення, наприклад, сповіщення про підключення чи помилку.
     */
    SYSTEM,

    /**
     * Повідомлення про тимчасове відключення від серверу облікового запису.
     */
    DISCONNECT_NOTIFICATION
}
