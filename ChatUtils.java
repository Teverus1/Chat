package org.teverus.chat.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ChatUtils {

    /**
     * Applies placeholders and translates color codes in a message.
     * This method is an example; you might have more complex placeholder logic.
     *
     * @param message The message to process.
     * @param playerName The player's name for placeholders (if used).
     * @return The processed message with placeholders and colors.
     */
    public static String applyPlaceholders(String message, String playerName) {
        // Example placeholder replacement (you might expand this)
        String processedMessage = message.replace("%playerName%", playerName)
                .replace("{player}", playerName); // Just in case, add {player} too

        // Этот метод не будет проверять пермишены, так как он для общих плейсхолдеров и цветов, которые всегда должны работать
        return ChatColor.translateAlternateColorCodes('&', processedMessage);
    }

    /**
     * Translates Minecraft alternative color codes ('&') to section signs ('§') based on player permissions.
     *
     * @param player The player for whom to translate the message.
     * @param message The message containing '&' color codes.
     * @return The message with translated color codes based on permissions.
     */
    public static String translateColorsWithPermissions(Player player, String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        StringBuilder translated = new StringBuilder();
        // Используем ChatColor.stripColor для очистки от существующих Bukkit §-кодов, чтобы избежать двойного перевода.
        // Это важно, так как TextComponent.fromLegacyText уже переводит §-коды, а мы хотим контролировать &-коды.
        String cleanedMessage = ChatColor.stripColor(message); // Удаляем любые Bukkit §-коды, если они уже есть

        for (int i = 0; i < cleanedMessage.length(); i++) {
            char c = cleanedMessage.charAt(i);
            if (c == '&' && i + 1 < cleanedMessage.length()) {
                char codeChar = Character.toLowerCase(cleanedMessage.charAt(i + 1));
                ChatColor chatColor = ChatColor.getByChar(codeChar);

                if (chatColor != null) {
                    // Проверка на обход всех ограничений (для админов)
                    if (player.hasPermission("chat.color.bypass") || player.hasPermission("chat.fullchatcontrol")) {
                        translated.append(chatColor);
                        i++; // Пропускаем следующий символ
                        continue;
                    }

                    boolean hasPermission = false;
                    if (chatColor.isColor()) {
                        // Пермишен на конкретный цвет или на все цвета
                        if (player.hasPermission("chat.color." + chatColor.name().toLowerCase()) || player.hasPermission("chat.color.*")) {
                            hasPermission = true;
                        }
                    } else if (chatColor.isFormat()) {
                        // Пермишен на конкретное форматирование или на все форматирования
                        if (player.hasPermission("chat.format." + chatColor.name().toLowerCase()) || player.hasPermission("chat.format.*")) {
                            hasPermission = true;
                        }
                    } else {
                        // Для Reset (&r), Magic (&k), Bold (&l), Strikethrough (&m), Underline (&n), Italic (&o)
                        // Если это не цвет и не формат (т.е. reset или legacy-форматы),
                        // решаем, давать ли по умолчанию или требовать пермишен.
                        // Здесь мы будем требовать пермишены на форматирование.
                        // Если это просто &r (reset), его обычно разрешают всем.
                        if (chatColor == ChatColor.RESET) {
                            hasPermission = true; // Сброс цвета разрешен всегда
                        }
                    }

                    if (hasPermission) {
                        translated.append(chatColor);
                        i++; // Пропускаем следующий символ
                    } else {
                        translated.append(c).append(cleanedMessage.charAt(i + 1)); // Оставляем код как есть, если нет пермишена
                        i++;
                    }
                } else {
                    translated.append(c); // Если это '&' но не код цвета/формата, оставляем как есть
                }
            } else {
                translated.append(c);
            }
        }
        return translated.toString();
    }

    /**
     * Old method, kept for compatibility if needed elsewhere.
     * Prefer translateColorsWithPermissions for chat messages.
     * Translates Minecraft alternative color codes ('&') to section signs ('§').
     *
     * @param message The message containing '&' color codes.
     * @return The message with translated color codes.
     */
    public static String translateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Removes all Minecraft color codes (both '&' and '§') from a string.
     * It first translates '&' codes to '§' codes, then strips all '§' codes.
     *
     * @param message The message from which to strip color codes.
     * @return The message without any color codes.
     */
    public static String stripColor(String message) {
        // First, ensure all '&' color codes are translated to '§'
        String translatedMessage = ChatColor.translateAlternateColorCodes('&', message);
        // Then, remove all '§' color codes
        return ChatColor.stripColor(translatedMessage);
    }
}