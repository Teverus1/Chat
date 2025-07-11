package org.teverus.chat.utils;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final ChatPlugin plugin;
    private PlayerDataManager playerDataManager; // Объявлено, но инициализируется через initialize()

    private final Pattern urlPattern = Pattern.compile("((https?):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");

    private final Map<UUID, UUID> lastMessaged = new HashMap<>();
    private final Map<UUID, Boolean> privateMessagesEnabled = new HashMap<>();

    // Настройки для форматирования ссылок
    private boolean enableLinkFormatting;
    private String linkFormatColor;
    private String linkHoverText;

    // Настройки звука broadcast
    private boolean broadcastSoundEnabled;
    private String broadcastSoundType;
    private float broadcastSoundVolume;
    private float broadcastSoundPitch; // ИСПРАВЛЕНО: Теперь правильное имя переменной и тип

    // Настройки звука игнорирования
    private boolean ignoreSoundEnabled;
    private String ignoreSoundType;
    private float ignoreSoundVolume;
    private float ignoreSoundPitch;

    public MessageManager(ChatPlugin plugin) {
        this.plugin = plugin;
        // НЕ ВЫЗЫВАЕМ loadSettings() ЗДЕСЬ. Это сделает ChatPlugin.onEnable() после всех инициализаций.
    }

    /**
     * Инициализирует MessageManager с необходимыми зависимостями.
     * Этот метод должен быть вызван из ChatPlugin.onEnable()
     * после того, как ChatPlugin.playerDataManager будет инициализирован.
     * @param playerDataManager экземпляр PlayerDataManager
     */
    public void initialize(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        loadSettings(); // Теперь безопасно загружаем настройки
    }

    /**
     * Загружает все настройки, используемые MessageManager, из конфигурации плагина.
     * Должен вызываться после инициализации ConfigManager и PlayerDataManager.
     */
    public void loadSettings() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (config == null) {
            plugin.getLogger().severe("Конфигурация в MessageManager.loadSettings() равна null! Не удалось загрузить настройки.");
            return;
        }

        this.enableLinkFormatting = config.getBoolean("chat.link-formatting.enabled", true);
        this.linkFormatColor = config.getString("chat.link-formatting.color", "&9&n");
        this.linkHoverText = config.getString("chat.link-formatting.hover-text", "&7Нажмите, чтобы открыть ссылку: &f%link%");

        this.broadcastSoundEnabled = config.getBoolean("broadcast.sound.enabled", true);
        this.broadcastSoundType = config.getString("broadcast.sound.type", "BLOCK_NOTE_BLOCK_PLING");
        this.broadcastSoundVolume = (float) config.getDouble("broadcast.sound.volume", 1.0);
        this.broadcastSoundPitch = (float) config.getDouble("broadcast.sound.pitch", 1.0); // Теперь имя переменной правильное

        this.ignoreSoundEnabled = config.getBoolean("ignore.sound-on-ignored.enabled", true);
        this.ignoreSoundType = config.getString("ignore.sound-on-ignored.type", "ENTITY_VILLAGER_NO");
        this.ignoreSoundVolume = (float) config.getDouble("ignore.sound-on-ignored.volume", 1.0);
        this.ignoreSoundPitch = (float) config.getDouble("ignore.sound-on-ignored.pitch", 1.0);

        plugin.getLogger().info("Настройки MessageManager перезагружены.");
    }

    /**
     * Перезагружает настройки для MessageManager.
     */
    public void reloadConfigSettings() {
        loadSettings();
    }


    // Проверяем, активированы ли личные сообщения для игрока
    public boolean isPrivateMessagesEnabled(Player player) {
        return privateMessagesEnabled.getOrDefault(player.getUniqueId(), true);
    }

    // Включаем или отключаем личные сообщения для игрока
    public void setPrivateMessagesEnabled(Player player, boolean enabled) {
        privateMessagesEnabled.put(player.getUniqueId(), enabled);
    }

    /**
     * Возвращает UUID последнего игрока, с которым общался данный игрок.
     * @param playerUUID UUID игрока, для которого ищем последнего собеседника.
     * @return UUID последнего собеседника, или null, если такого нет.
     */
    public UUID getLastMessaged(UUID playerUUID) {
        return lastMessaged.get(playerUUID);
    }

    // --- Методы для форматирования ссылок ---
    public String formatMessageWithLinks(String message, Player player) {
        String formatted = ChatUtils.translateColors(message);

        // Проверяем, включено ли форматирование ссылок и имеет ли игрок обходной пермишен
        if (enableLinkFormatting && !player.hasPermission("chat.linkformatting.bypass")) {
            Matcher matcher = urlPattern.matcher(formatted);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String url = matcher.group();
                String hoverText = ChatUtils.translateColors(linkHoverText.replace("%link%", url));
                String replacement = ChatUtils.translateColors(linkFormatColor + url + ChatColor.RESET);
                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);
            formatted = sb.toString();
        }
        return formatted;
    }

    // Добавлены геттеры для доступа к настройкам broadcast (если требуются извне)
    public boolean isBroadcastSoundEnabled() { return broadcastSoundEnabled; }
    public String getBroadcastSoundType() { return broadcastSoundType; }
    public float getBroadcastSoundVolume() { return broadcastSoundVolume; }
    public float getBroadcastSoundPitch() { return broadcastSoundPitch; } // Теперь геттер соответствует правильному имени переменной


    // --- Методы для звуков Broadcast ---
    public void playBroadcastSound(Player sender) {
        if (!broadcastSoundEnabled) {
            return;
        }

        Sound sound = null;
        try {
            sound = Sound.valueOf(broadcastSoundType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неизвестный тип звука для broadcast в конфиге: " + broadcastSoundType + ". Используется дефолтный звук.");
            sound = Sound.BLOCK_NOTE_BLOCK_PLING; // Дефолтный звук
        }

        if (sound != null) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.playSound(p.getLocation(), sound, broadcastSoundVolume, broadcastSoundPitch);
            }
        }
    }


    // Обработка личных сообщений
    public void sendMessage(Player sender, Player receiver, String message) {
        // Запрещаем отправлять себе сообщения
        if (sender.equals(receiver)) {
            sender.sendMessage(plugin.getMessage("error-messages.target-is-sender"));
            return;
        }

        // --- Проверка на игнорирование ---
        // Использование getMessage для "ignored-by-target-message"
        if (playerDataManager.isIgnoring(receiver.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(plugin.getMessage("ignore.ignored-by-target-message").replace("%targetName%", receiver.getName())); //

            if (ignoreSoundEnabled) {
                try {
                    Sound sound = Sound.valueOf(ignoreSoundType.toUpperCase());
                    sender.playSound(sender.getLocation(), sound, ignoreSoundVolume, ignoreSoundPitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неизвестный тип звука для игнорирования: " + ignoreSoundType);
                }
            }
            plugin.getChatLogger().logChatMessage(message, sender.getName(), receiver.getName(), "PRIVATE_BLOCKED_BY_IGNORE");
            return;
        }
        // --- КОНЕЦ: Проверка на игнорирование ---


        // Проверка на отключенные личные сообщения у получателя
        // Использование getMessage для "private-messages.receiver-deactivated-message"
        if (!isPrivateMessagesEnabled(receiver)) {
            sender.sendMessage(plugin.getMessage("private-messages.receiver-deactivated-message"));
            return;
        }

        // Форматируем сообщение для отправителя
        // Использование getMessage для "private-messages.format-sent"
        String senderFormattedMessage = plugin.getMessage("private-messages.format-sent")
                .replace("%recipient_name%", receiver.getName())
                .replace("%message%", message);


        // Форматируем сообщение для получателя
        // Использование getMessage для "private-messages.format-received"
        String receiverFormattedMessage = plugin.getMessage("private-messages.format-received")
                .replace("%sendername%", sender.getName()) // ИСПРАВЛЕНО: плейсхолдер %sendername% соответствует коду
                .replace("%message%", message);


        sender.sendMessage(senderFormattedMessage);
        receiver.sendMessage(receiverFormattedMessage);

        // Запоминаем последнего собеседника для команды /r
        lastMessaged.put(sender.getUniqueId(), receiver.getUniqueId());
        lastMessaged.put(receiver.getUniqueId(), sender.getUniqueId());

        // Логируем оригинальное сообщение через централизованный метод
        plugin.getChatLogger().logChatMessage(message, sender.getName(), receiver.getName(), "PRIVATE");
    }

    /**
     * Обрабатывает команду ответа на последнее сообщение.
     * @param sender Игрок, отправляющий ответ.
     * @param message Содержание сообщения.
     */
    public void reply(Player sender, String message) {
        UUID lastSenderUUID = lastMessaged.get(sender.getUniqueId());

        if (lastSenderUUID == null) {
            sender.sendMessage(plugin.getMessage("private-messages.no-one-to-reply"));
            return;
        }

        Player lastSender = plugin.getServer().getPlayer(lastSenderUUID);

        if (lastSender == null || !lastSender.isOnline()) {
            sender.sendMessage(plugin.getMessage("private-messages.target-not-online")); // ИСПРАВЛЕНО: полный путь к сообщению
            lastMessaged.remove(sender.getUniqueId());
            return;
        }

        sendMessage(sender, lastSender, message);
    }
}