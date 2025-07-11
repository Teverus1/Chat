package org.teverus.chat.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor; // ChatColor здесь не используется, можно удалить, но пользователь просил не вырезать лишнее
import org.teverus.chat.ChatPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Level; // Level здесь не используется, можно удалить, но пользователь просил не вырезать лишнее
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ChatLogger {

    private final ChatPlugin plugin;
    private BufferedWriter writer;
    private File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public ChatLogger(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Создает или открывает файл лога. Если файл chat_log.log уже существует,
     * он будет заархивирован перед созданием нового пустого файла.
     */
    public void createNewLogFile() {
        File logDirectory = new File(plugin.getDataFolder(), "logs/chat");
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }

        String logFileName = "chat_log.log";
        logFile = new File(logDirectory, logFileName);

        // Если файл лога уже существует, сначала архивируем его
        if (logFile.exists()) {
            archiveLogFile(); // Вызываем метод архивации
        }

        try {
            // Открываем файл для дозаписи. Если не существует, FileWriter создаст его.
            // Используем true для append mode
            FileWriter fw = new FileWriter(logFile, true);
            this.writer = new BufferedWriter(fw);
            //plugin.getLogger().info("Новый лог-файл чата создан: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось создать или открыть файл лога: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Логирует сообщение чата в файл и выводит его в консоль сервера.
     *
     * @param originalMessage Оригинальное сообщение игрока (без форматирования и цветов).
     * @param senderName      Имя отправителя сообщения (может быть null для BROADCAST).
     * @param receiverName    Имя получателя сообщения (для приватных), null для остальных.
     * @param chatType        Тип чата (PRIVATE, LOCAL, GLOBAL, BROADCAST, LOCAL_RP_ME, GLOBAL_RP_GTRY, ANY_CHAT, etc.).
     */
    public void logChatMessage(String originalMessage, String senderName, String receiverName, String chatType) {
        // Если writer null, пытаемся создать новый файл
        if (writer == null) {
            createNewLogFile(); // Попытка пересоздать writer, если он null
            if (writer == null) { // Если все равно null после попытки, значит ошибка, и логировать не можем
                plugin.getLogger().warning("ChatLogger writer is null, cannot log message.");
                return;
            }
        }

        String consoleLogMessage;
        String fileLogMessage;

        // Для записи в файл используем оригинальное сообщение, очищенное от всех цветовых кодов.
        String cleanedMessageForFile = ChatUtils.stripColor(originalMessage);

        String timestamp = dateFormat.format(new Date());

        // Получаем форматы из ChatPlugin.getMessage(), а не напрямую из getConfig()
        // Предполагается, что ChatPlugin.getMessage() уже умеет искать в "console-log-formats."
        String privateFormat = plugin.getConsoleLogFormat("private-message");
        String localFormat = plugin.getConsoleLogFormat("local-chat");
        String globalFormat = plugin.getConsoleLogFormat("global-chat");
        String broadcastFormat = plugin.getConsoleLogFormat("broadcast");
        String localRpFormat = plugin.getConsoleLogFormat("local-rp");
        String globalRpFormat = plugin.getConsoleLogFormat("global-rp");
        String unknownFormat = plugin.getConsoleLogFormat("unknown");
        String anyChatFormat = plugin.getConsoleLogFormat("any-chat");


        String rpCommandForLog = "";
        String rpChatTypeName = ""; // для файла, чтобы было LOCAL_RP/COMMAND

        // Обработка RP-команд для получения их имени и форматирования
        String normalizedChatType = chatType.toUpperCase();
        if (normalizedChatType.startsWith("LOCAL_RP_")) {
            rpChatTypeName = "LOCAL_RP";
            // Извлекаем только последнюю часть (ME, DO, TRY, TODO)
            String[] parts = normalizedChatType.split("_", 3); // Ограничиваем сплит до 3 частей
            if (parts.length == 3) {
                rpCommandForLog = "/" + parts[2].toLowerCase(); // /me, /do и т.д.
            }
        } else if (normalizedChatType.startsWith("GLOBAL_RP_")) {
            rpChatTypeName = "GLOBAL_RP";
            String[] parts = normalizedChatType.split("_", 3);
            if (parts.length == 3) {
                rpCommandForLog = "/" + parts[2].toLowerCase(); // /gme, /gdo и т.д.
            }
        }

        // Выбираем формат для консоли и формируем строку для файла
        switch (normalizedChatType) { // Используем нормализованный chatType
            case "PRIVATE":
            case "PRIVATE_BLOCKED_BY_IGNORE": // Добавим обработку этого типа
                consoleLogMessage = ChatUtils.translateColors(privateFormat
                        .replace("%sender%", Objects.requireNonNull(senderName, "Sender name cannot be null for PRIVATE message"))
                        .replace("%receiver%", Objects.requireNonNull(receiverName, "Receiver name cannot be null for PRIVATE message"))
                        .replace("%message%", originalMessage));
                fileLogMessage = String.format("[%s] [PRIVATE] [%s -> %s]: %s", timestamp, senderName, receiverName, cleanedMessageForFile);
                break;
            case "LOCAL":
            case "LOCAL_CHAT": // Если вы используете "LOCAL_CHAT" для типа, добавьте его
                consoleLogMessage = ChatUtils.translateColors(localFormat
                        .replace("%player%", Objects.requireNonNull(senderName, "Sender name cannot be null for LOCAL message"))
                        .replace("%message%", originalMessage));
                fileLogMessage = String.format("[%s] [LOCAL] [%s]: %s", timestamp, senderName, cleanedMessageForFile);
                break;
            case "GLOBAL":
            case "GLOBAL_CHAT": // Если вы используете "GLOBAL_CHAT" для типа, добавьте его
                consoleLogMessage = ChatUtils.translateColors(globalFormat
                        .replace("%player%", Objects.requireNonNull(senderName, "Sender name cannot be null for GLOBAL message"))
                        .replace("%message%", originalMessage));
                fileLogMessage = String.format("[%s] [GLOBAL] [%s]: %s", timestamp, senderName, cleanedMessageForFile);
                break;
            case "BROADCAST":
                consoleLogMessage = ChatUtils.translateColors(broadcastFormat
                        .replace("%message%", originalMessage));
                fileLogMessage = String.format("[%s] [BROADCAST]: %s", timestamp, cleanedMessageForFile);
                break;
            case "LOCAL_RP_ME":
            case "LOCAL_RP_DO":
            case "LOCAL_RP_TRY":
            case "LOCAL_RP_TODO":
                consoleLogMessage = ChatUtils.translateColors(localRpFormat
                        .replace("%player%", Objects.requireNonNull(senderName, "Sender name cannot be null for LOCAL_RP message"))
                        .replace("%message%", originalMessage)
                        .replace("%command%", rpCommandForLog));
                // Для файла используем более читабельный формат
                fileLogMessage = String.format("[%s] [%s%s] [%s]: %s", timestamp, rpChatTypeName, rpCommandForLog.toUpperCase(), senderName, cleanedMessageForFile);
                break;
            case "GLOBAL_RP_GME":
            case "GLOBAL_RP_GDO":
            case "GLOBAL_RP_GTRY":
            case "GLOBAL_RP_GTODO":
                consoleLogMessage = ChatUtils.translateColors(globalRpFormat
                        .replace("%player%", Objects.requireNonNull(senderName, "Sender name cannot be null for GLOBAL_RP message"))
                        .replace("%message%", originalMessage)
                        .replace("%command%", rpCommandForLog));
                fileLogMessage = String.format("[%s] [%s%s] [%s]: %s", timestamp, rpChatTypeName, rpCommandForLog.toUpperCase(), senderName, cleanedMessageForFile);
                break;
            case "ANY_CHAT":
                consoleLogMessage = ChatUtils.translateColors(anyChatFormat
                        .replace("%player%", Objects.requireNonNull(senderName, "Sender name cannot be null for ANY_CHAT message"))
                        .replace("%message%", originalMessage));
                fileLogMessage = String.format("[%s] [ANY_CHAT] [%s]: %s", timestamp, senderName, cleanedMessageForFile);
                break;
            default:
                consoleLogMessage = ChatUtils.translateColors(unknownFormat
                        .replace("%player%", senderName != null ? senderName : "N/A")
                        .replace("%message%", originalMessage));
                fileLogMessage = String.format("[%s] [UNKNOWN] [%s]: %s", timestamp, senderName != null ? senderName : "N/A", cleanedMessageForFile);
                break;
        }

        // Вывод в консоль
        Bukkit.getConsoleSender().sendMessage(consoleLogMessage);

        // Записываем сообщение в текстовый файл
        try {
            writer.write(fileLogMessage);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось записать сообщение в лог-файл: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Логирует административные действия или другие события, не связанные напрямую с сообщениями чата.
     *
     * @param actionMessage Сообщение о действии, которое нужно залогировать.
     */
    public void logAction(String actionMessage) {
        if (writer == null) {
            createNewLogFile(); // Попытка пересоздать writer, если он null
            if (writer == null) {
                plugin.getLogger().warning("ChatLogger writer is null, cannot log action.");
                return;
            }
        }

        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [ACTION]: %s", timestamp, actionMessage);

        try {
            writer.write(logEntry);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось записать действие в лог-файл: " + e.getMessage());
            e.printStackTrace();
        }
        // Также выводим в консоль сервера, чтобы администраторы видели действия
        plugin.getLogger().info("[ChatLogger Action]: " + actionMessage);
    }

    /**
     * Архивирует существующий файл лога (chat_log.log) в zip-архив с датой и временем.
     */
    public void archiveLogFile() {
        if (logFile == null || !logFile.exists()) {
            return;
        }

        closeLogFile();

        String archiveFileName = fileDateFormat.format(new Date()) + "_chat_log.zip";
        File zipFile = new File(logFile.getParentFile(), archiveFileName);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
             FileInputStream fis = new FileInputStream(logFile)) {

            ZipEntry zipEntry = new ZipEntry(logFile.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.closeEntry();
            plugin.getLogger().info("Лог-файл '" + logFile.getName() + "' успешно заархивирован в '" + zipFile.getName() + "'");
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при архивации лог-файла: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (logFile.exists()) {
                if (logFile.delete()) {
                    //plugin.getLogger().info("Оригинальный лог-файл удален после архивации.");
                } else {
                    plugin.getLogger().warning("Не удалось удалить оригинальный лог-файл после архивации.");
                }
            }
        }
    }

    /**
     * Закрывает BufferedWriter.
     */
    public void closeLogFile() {
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при закрытии лог-файла: " + e.getMessage());
            e.printStackTrace();
        }
    }
}