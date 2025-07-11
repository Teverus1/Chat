// src/main/java/org/teverus/chat/utils/Permissions.java
package org.teverus.chat.utils;

public class Permissions {
    // Базовое разрешение для вашего плагина. Все остальные будут начинаться с него.
    public static final String BASE = "chat.";

    // Разрешения для форматирования чата
    public static final String CHAT_COLOR = BASE + "color";         // Разрешает использовать цветовые коды (например, &c)
    public static final String CHAT_BOLD = BASE + "bold";           // Разрешает использовать жирный шрифт
    public static final String CHAT_ITALIC = BASE + "italic";         // Разрешает использовать курсив
    public static final String CHAT_MAGIC = BASE + "magic";         // Разрешает использовать "волшебный" (запутанный) текст
    public static final String CHAT_UNDERLINE = BASE + "underline"; // Подчеркнутый текст
    public static final String CHAT_STRIKETHROUGH = BASE + "strikethrough"; // Зачеркнутый текст

    // Разрешения для обхода проверок чата
    public static final String CHAT_BYPASS_COOLDOWN = BASE + "bypass.cooldown";       // Обход задержки между сообщениями
    public static final String CHAT_BYPASS_SPAM = BASE + "bypass.spam";             // Обход анти-спам системы
    public static final String CHAT_BYPASS_PROFANITY = BASE + "bypass.profanity";   // Обход фильтра нецензурных слов
    public static final String CHAT_BYPASS_MESSAGELENGTH = BASE + "bypass.messagelength"; // Обход ограничения длины сообщения
    public static final String CHAT_BYPASS_ADVERTISING = BASE + "bypass.advertising"; // Обход фильтра рекламы

    // Разрешения для команд (примеры)
    public static final String COMMAND_BROADCAST = BASE + "command.broadcast";       // Команда /broadcast
    public static final String COMMAND_CLEARCHAT = BASE + "command.clearchat";       // Команда /clearchat
    public static final String COMMAND_GLOBALCHAT_TOGGLE = BASE + "command.globalchat.toggle"; // Команда для переключения глобального чата
    public static final String COMMAND_GLOBALCHAT_SEE = BASE + "command.globalchat.see";     // Видеть глобальный чат, если он отключен для обычных игроков (для стаффа)
    public static final String COMMAND_IGNORE = BASE + "command.ignore";           // Команда /ignore
    public static final String COMMAND_MESSAGE = BASE + "command.message";         // Команда /message (личные сообщения)
    public static final String COMMAND_RELOAD = BASE + "command.reload";           // Команда перезагрузки плагина/конфига (очень важна)
    public static final String COMMAND_REPLY = BASE + "command.reply";             // Команда /reply
    public static final String COMMAND_TOGGLE_PM = BASE + "command.togglepm";       // Команда /togglepm (вкл/выкл личные сообщения)
    public static final String COMMAND_UNIGNORE = BASE + "command.unignore";         // Команда /unignore
    // Добавьте больше для каждой вашей команды

    // Разрешения для сообщений входа/выхода
    public static final String JOIN_MESSAGE_CUSTOM = BASE + "joinmessage.custom"; // Разрешает игроку иметь кастомное сообщение входа
    public static final String QUIT_MESSAGE_CUSTOM = BASE + "quitmessage.custom"; // Разрешает игроку иметь кастомное сообщение выхода

    // Разрешения для модераторов/администраторов (общие)
    public static final String ADMIN_VIEW_IGNORED_MESSAGES = BASE + "admin.viewignored"; // Для стаффа, чтобы видеть сообщения игроков, которые игнорируют друг друга.

    // Разрешение-заглушка для всех разрешений плагина (например, chat.*).
    public static final String ALL = BASE + "*";
}