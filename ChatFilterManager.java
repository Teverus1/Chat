package org.teverus.chat.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap; // Рекомендуется для Map, используемых из разных потоков

public class ChatFilterManager {

    private final ChatPlugin plugin;
    // Рекомендуется ConcurrentHashMap для потокобезопасности, если эти мапы обновляются из AsyncPlayerChatEvent
    public final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> spamCounter = new ConcurrentHashMap<>();

    private boolean antiFloodEnabled;
    private long antiFloodCooldown;
    private String antiFloodBypassPermission;
    private boolean antiSpamEnabled;
    private int antiSpamMessageLimit;
    private boolean antiSpamKickOnSpam;
    private String antiSpamBypassPermission;

    private boolean badWordsFilterEnabled;
    private List<String> badWords;
    private String badWordsReplacement;

    private boolean linkFormattingEnabled;
    private String linkColor;
    private String linkHoverText;

    public ChatFilterManager(ChatPlugin plugin) {
        this.plugin = plugin;
        loadSettings();
    }

    public void loadSettings() {
        this.antiFloodEnabled = plugin.getConfigManager().getConfig().getBoolean("chat.anti-flood.enabled", true);
        this.antiFloodCooldown = plugin.getConfigManager().getConfig().getLong("chat.anti-flood.cooldown-milliseconds", 1000L);
        this.antiFloodBypassPermission = plugin.getConfigManager().getConfig().getString("chat.anti-flood.bypass-permission", "chatplugin.antiflood.bypass");

        this.antiSpamEnabled = plugin.getConfigManager().getConfig().getBoolean("chat.anti-spam.enabled", true);
        this.antiSpamMessageLimit = plugin.getConfigManager().getConfig().getInt("chat.anti-spam.message-limit", 3);
        this.antiSpamKickOnSpam = plugin.getConfigManager().getConfig().getBoolean("chat.anti-spam.kick-on-spam", false);
        this.antiSpamBypassPermission = plugin.getConfigManager().getConfig().getString("chat.anti-spam.bypass-permission", "chatplugin.antispam.bypass");

        this.badWordsFilterEnabled = plugin.getConfigManager().getConfig().getBoolean("chat.bad-words-filter.enabled", false);
        this.badWords = plugin.getConfigManager().getConfig().getStringList("chat.bad-words-filter.words");
        this.badWordsReplacement = plugin.getConfigManager().getConfig().getString("chat.bad-words-filter.replacement", "****");

        this.linkFormattingEnabled = plugin.getConfigManager().getConfig().getBoolean("chat.link-formatting.enabled", true);
        this.linkColor = plugin.getConfigManager().getConfig().getString("chat.link-formatting.color", "&9&n");
        this.linkHoverText = plugin.getConfigManager().getConfig().getString("chat.link-formatting.hover-text", "&7Нажмите, чтобы открыть ссылку: &f%link%");

        plugin.getLogger().info("Настройки ChatFilterManager загружены/перезагружены.");
        plugin.getLogger().info("  Анти-флуд: " + (antiFloodEnabled ? "Включен, Кулдаун: " + antiFloodCooldown + "мс" : "Отключен"));
        plugin.getLogger().info("  Анти-спам: " + (antiSpamEnabled ? "Включен, Лимит повторов: " + antiSpamMessageLimit + (antiSpamKickOnSpam ? " (Кик)" : " (Предупреждение)") : "Отключен"));
        plugin.getLogger().info("  Фильтр слов: " + (badWordsFilterEnabled ? "Включен" : "Отключен"));
        plugin.getLogger().info("  Форматирование ссылок (логика не в этом классе): " + (linkFormattingEnabled ? "Включено" : "Отключено"));
    }

    public void reloadConfigSettings() {
        loadSettings();
    }

    public boolean checkAntiFlood(Player player) {
        if (!antiFloodEnabled) {
            return true;
        }
        if (player.hasPermission(antiFloodBypassPermission)) {
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastTime < antiFloodCooldown) {
            return false;
        }
        lastMessageTime.put(player.getUniqueId(), currentTime);
        return true;
    }

    /**
     * Проверяет сообщение игрока на соответствие правилам анти-спама.
     * Увеличивает счетчик спама, если сообщение повторяет предыдущее.
     * Сбрасывает счетчик, если сообщение отличается.
     *
     * @param player Игрок, отправляющий сообщение.
     * @param message Содержание сообщения.
     * @return true, если сообщение НЕ является спамом; false, если является.
     */
    public boolean checkAntiSpam(Player player, String message) {
        if (!antiSpamEnabled) {
            return true;
        }
        if (player.hasPermission(antiSpamBypassPermission)) {
            return true;
        }

        String lastContent = lastMessageContent.getOrDefault(player.getUniqueId(), "");
        int currentSpamCount = spamCounter.getOrDefault(player.getUniqueId(), 0);

        // Сравниваем сообщения без цветовых кодов для точного определения повторов
        if (ChatUtils.stripColor(lastContent).equalsIgnoreCase(ChatUtils.stripColor(message))) {
            currentSpamCount++;
            if (currentSpamCount >= antiSpamMessageLimit) {
                spamCounter.put(player.getUniqueId(), currentSpamCount); // Обновляем счетчик, даже если это спам
                return false; // Является спамом
            }
        } else {
            currentSpamCount = 0; // Сбрасываем счетчик, если сообщение изменилось
        }

        lastMessageContent.put(player.getUniqueId(), message);
        spamCounter.put(player.getUniqueId(), currentSpamCount);
        return true; // Не является спамом (пока)
    }


    /**
     * Проверяет, является ли данное сообщение спамом для игрока,
     * основываясь на предыдущих сообщениях. Этот метод НЕ обновляет внутренние счетчики спама.
     * Используется для запроса статуса спама без изменения состояния.
     * Для проверки и обновления счетчиков используйте {@link #checkAntiSpam(Player, String)}.
     *
     * @param playerUUID UUID игрока, чье сообщение проверяется.
     * @param message Содержание сообщения.
     * @return true, если сообщение считается спамом, иначе false.
     */
    public boolean isSpamMessage(java.util.UUID playerUUID, String message) { // Изменен тип первого аргумента на UUID
        if (!antiSpamEnabled) {
            return false; // Не спам, если анти-спам отключен
        }

        // В этом методе, так как у нас нет объекта Player, мы не можем проверить пермишен.
        // Предполагается, что проверка пермишена обхода спама будет выполнена в AntiSpamListener
        // перед вызовом этого метода, если это необходимо для логики.
        // Если вы хотите, чтобы этот метод сам проверял пермишен, ему нужен объект Player.
        // Для текущей структуры, где AntiSpamListener уже делает эту проверку:
        // if (player.hasPermission(antiSpamBypassPermission)) return false; // Эта строка не может быть здесь без Player

        String lastContent = lastMessageContent.getOrDefault(playerUUID, "");
        int currentSpamCount = spamCounter.getOrDefault(playerUUID, 0);

        // Сравниваем сообщения без цветовых кодов для точного определения повторов
        if (ChatUtils.stripColor(lastContent).equalsIgnoreCase(ChatUtils.stripColor(message))) {
            // Если сообщение повторяется, проверяем, достигнет ли счетчик лимита
            return (currentSpamCount + 1) >= antiSpamMessageLimit;
        }
        return false; // Сообщение не повторяется, поэтому не спам
    }


    /**
     * Применяет фильтрацию плохих слов к сообщению.
     * Этот метод пытается сохранить цветовые коды, насколько это возможно,
     * при замене плохих слов, игнорируя регистр.
     *
     * @param message Исходное сообщение.
     * @return Отфильтрованное сообщение.
     */
    public String filterMessage(String message) {
        if (!badWordsFilterEnabled || badWords == null || badWords.isEmpty()) {
            return message;
        }

        String processedMessage = message;

        for (String badWord : badWords) {
            // Экранируем спецсимволы в плохом слове для использования в regex
            String escapedBadWord = Pattern.quote(badWord);

            // Заменяем все вхождения, игнорируя регистр.
            // Это не будет сохранять цвета внутри самого слова, но сохранит цвета до и после.
            // Например, "привет &cплохое&fслово тест" -> "привет &c****&f тест" (если replacement не содержит цветов).
            processedMessage = processedMessage.replaceAll("(?i)" + escapedBadWord, Matcher.quoteReplacement(badWordsReplacement));
        }
        return processedMessage;
    }


    public void resetFiltersForPlayer(Player player) {
        lastMessageTime.remove(player.getUniqueId());
        lastMessageContent.remove(player.getUniqueId());
        spamCounter.remove(player.getUniqueId());
        plugin.getLogger().fine("Счетчики фильтров для игрока " + player.getName() + " сброшены.");
    }

    public boolean isAntiSpamEnabled() {
        return antiSpamEnabled;
    }

    public boolean isAntiSpamKickOnSpam() {
        return antiSpamKickOnSpam;
    }

    public boolean isAntiFloodEnabled() {
        return antiFloodEnabled;
    }

    // --- ДОБАВЛЕННЫЕ ГЕТТЕРЫ ---

    /**
     * Проверяет, включен ли фильтр плохих слов.
     * @return true, если фильтр включен, иначе false.
     */
    public boolean isWordFilterEnabled() {
        return badWordsFilterEnabled;
    }

    /**
     * Возвращает лимит сообщений для анти-спама.
     * @return Лимит сообщений.
     */
    public int getAntiSpamMessageLimit() {
        return antiSpamMessageLimit;
    }

    /**
     * Возвращает кулдаун анти-флуда в миллисекундах.
     * @return Кулдаун в мс.
     */
    public long getAntiFloodCooldown() {
        return antiFloodCooldown;
    }

    // --- КОНЕЦ ДОБАВЛЕННЫХ ГЕТТЕРОВ ---
}