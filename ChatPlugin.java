package org.teverus.chat;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;

import org.teverus.chat.commands.*;
import org.teverus.chat.listeners.GlobalChatListener;
import org.teverus.chat.listeners.LocalChatListener;
import org.teverus.chat.listeners.PlayerJoinQuitListener;
import org.teverus.chat.listeners.AntiSpamListener;
import org.teverus.chat.listeners.ChatMonitorListener; // Оставлен, но стоит рассмотреть его полезность

import org.teverus.chat.utils.BroadcastManager;
import org.teverus.chat.utils.ChatLogger;
import org.teverus.chat.utils.ChatUtils;
import org.teverus.chat.utils.MessageManager;
import org.teverus.chat.utils.PlayerDataManager;
import org.teverus.chat.utils.JoinQuitMessageManager;
import org.teverus.chat.utils.ChatFilterManager;
import org.teverus.chat.utils.ConfigManager;

import java.util.UUID;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import java.util.Objects;

import org.teverus.adventoraBans.AdventoraBans; // ПРЕДПОЛАГАЕМЫЙ путь к основному классу AdventoraBans

public final class ChatPlugin extends JavaPlugin {

    private ChatLogger chatLogger;
    private PlayerDataManager playerDataManager;
    private BroadcastManager broadcastManager;
    private MessageManager messageManager;
    private JoinQuitMessageManager joinQuitMessageManager;
    private ChatFilterManager chatFilterManager;
    private ConfigManager configManager;

    private String pluginPrefixRaw;
    private String pluginPrefixTranslated;

    // Переменная для хранения ссылки на AdventoraBans
    private AdventoraBans adventoraBansPlugin;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.setupConfig(); // Создаем/загружаем config.yml в первую очередь

        // Загрузка префикса плагина при включении
        loadPluginSettings();

        chatLogger = new ChatLogger(this);
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.loadAllPlayerData(); // Загрузка данных при старте (ВАЖНО: ДО MessageManager)

        broadcastManager = new BroadcastManager(this);
        messageManager = new MessageManager(this);
        messageManager.initialize(playerDataManager); // ПЕРЕДАЕМ playerDataManager в MessageManager

        joinQuitMessageManager = new JoinQuitMessageManager(this, playerDataManager);
        chatFilterManager = new ChatFilterManager(this); // Инициализируем ChatFilterManager

        chatLogger.createNewLogFile();

        // Проверяем, загружен ли AdventoraBans
        Plugin pluginInstance = getServer().getPluginManager().getPlugin("AdventoraBans");
        if (pluginInstance instanceof AdventoraBans) {
            this.adventoraBansPlugin = (AdventoraBans) pluginInstance;
            getLogger().info("AdventoraBans найден и подключен.");
        } else {
            getLogger().warning("AdventoraBans не найден! Функционал мутов не будет работать. Убедитесь, что плагин установлен и работает.");
        }

        // --- Регистрация слушателей событий ---
        // Теперь передаем ссылку на AdventoraBans напрямую, если она есть
        getServer().getPluginManager().registerEvents(new AntiSpamListener(this, chatFilterManager), this);
        getServer().getPluginManager().registerEvents(new LocalChatListener(this), this); // LocalChatListener теперь сам получает AdventoraBans через плагин
        getServer().getPluginManager().registerEvents(new GlobalChatListener(this), this); // GlobalChatListener теперь сам получает AdventoraBans через плагин
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatMonitorListener(this), this);

        registerCommands();

        getLogger().info("ChatPlugin успешно включен!");
    }

    @Override
    public void onDisable() {
        if (chatLogger != null) {
            chatLogger.archiveLogFile();
            chatLogger.closeLogFile();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        if (configManager != null) {
            configManager.saveConfig(); // Убеждаемся, что конфиг сохраняется при выключении
        }
        getLogger().info("ChatPlugin успешно выключен!");
    }

    /**
     * Загружает основные настройки плагина, включая префикс.
     * Вынесено в отдельный метод для использования в onEnable и reloadConfig.
     */
    private void loadPluginSettings() {
        // Всегда используем ConfigManager для доступа к конфигу
        if (configManager != null && configManager.getConfig() != null) {
            this.pluginPrefixRaw = configManager.getConfig().getString("chat.prefix", "&7[&bChat&7]");
        } else {
            // Запасной вариант, если ConfigManager почему-то не инициализирован или конфиг не загружен
            getLogger().warning("ConfigManager или его конфиг не инициализирован при загрузке настроек! Использован Bukkit getConfig() как запасной вариант.");
            this.pluginPrefixRaw = getConfig().getString("chat.prefix", "&7[&bChat&7]");
        }
        this.pluginPrefixTranslated = ChatUtils.translateColors(this.pluginPrefixRaw);
    }


    @Override
    public void reloadConfig() {
        // Перезагружаем основной конфиг файл (Bukkit) через ConfigManager
        if (configManager != null) {
            configManager.reloadConfig(); // Перезагружает YamlConfiguration из файла
            loadPluginSettings(); // Перезагружаем префикс плагина после обновления конфига
        } else {
            getLogger().warning("ConfigManager не инициализирован при вызове reloadConfig! Не могу перезагрузить конфиг полностью.");
            super.reloadConfig(); // Запасной вариант, перезагружаем только Bukkit's internal config
            this.pluginPrefixRaw = getConfig().getString("chat.prefix", "&7[&bChat&7]");
            this.pluginPrefixTranslated = ChatUtils.translateColors(this.pluginPrefixRaw);
        }

        // Перезагружаем настройки менеджеров
        if (messageManager != null) {
            messageManager.reloadConfigSettings();
        }
        if (chatFilterManager != null) {
            chatFilterManager.reloadConfigSettings();
        }
        if (joinQuitMessageManager != null) {
            joinQuitMessageManager.reloadConfigSettings();
        }
        if (broadcastManager != null) {
            broadcastManager.reloadConfigSettings();
        }
        getLogger().info("Конфигурация ChatPlugin перезагружена!");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("global")).setExecutor(new GlobalChatCommand(this));
        Objects.requireNonNull(getCommand("broadcast")).setExecutor(new BroadcastCommand(this));
        Objects.requireNonNull(getCommand("chatreload")).setExecutor(new ReloadCommand(this));
        Objects.requireNonNull(getCommand("prefix")).setExecutor(new PrefixCommand(this, playerDataManager));

        Objects.requireNonNull(getCommand("msg")).setExecutor(new MessageCommand(this, messageManager));
        Objects.requireNonNull(getCommand("tell")).setExecutor(new MessageCommand(this, messageManager));
        Objects.requireNonNull(getCommand("m")).setExecutor(new MessageCommand(this, messageManager));
        Objects.requireNonNull(getCommand("reply")).setExecutor(new ReplyCommand(this, messageManager));
        Objects.requireNonNull(getCommand("r")).setExecutor(new ReplyCommand(this, messageManager));
        Objects.requireNonNull(getCommand("toggleprivatemessages")).setExecutor(new TogglePrivateMessagesCommand(this, messageManager));

        Objects.requireNonNull(getCommand("ignore")).setExecutor(new IgnoreCommand(this, playerDataManager));
        Objects.requireNonNull(getCommand("unignore")).setExecutor(new UnignoreCommand(this, playerDataManager));

        Objects.requireNonNull(getCommand("me")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("do")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("try")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("todo")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("gme")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("gdo")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("gtry")).setExecutor(new RPCommandExecutor(this));
        Objects.requireNonNull(getCommand("gtodo")).setExecutor(new RPCommandExecutor(this));

        Objects.requireNonNull(getCommand("joinmessage")).setExecutor(new JoinMessageCommand(this, joinQuitMessageManager));
        Objects.requireNonNull(getCommand("quitmessage")).setExecutor(new QuitMessageCommand(this, joinQuitMessageManager));

        // --- ДОБАВЛЯЕМ НОВУЮ КОМАНДУ /clearchat ЗДЕСЬ ---
        Objects.requireNonNull(getCommand("clearchat")).setExecutor(new ClearChatCommand(this));
        // --- КОНЕЦ ДОБАВЛЕНИЯ НОВОЙ КОМАНДЫ ---
    }

    public ChatLogger getChatLogger() {
        return chatLogger;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public JoinQuitMessageManager getJoinQuitMessageManager() {
        return joinQuitMessageManager;
    }

    public ChatFilterManager getChatFilterManager() {
        return chatFilterManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public String getPluginPrefixRaw() {
        return this.pluginPrefixRaw;
    }

    public String getPluginPrefixTranslated() {
        return this.pluginPrefixTranslated;
    }

    @Deprecated
    public String getPluginPrefix() {
        return this.pluginPrefixTranslated;
    }

    // Все геттеры конфигурации теперь явно используют getConfigManager().getConfig()
    public String getLocalChatFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("chat.local-chat-format", "&7{prefix}&r {player} &7говорит локально: {message}"));
    }

    public String getGlobalChatFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("chat.global-chat-format", "&8[Глобальный] &7{prefix}&r {player}: {message}"));
    }

    public int getLocalChatRadius() {
        return configManager.getConfig().getInt("chat.local-chat-radius", 50);
    }

    public int getLocalRpRadius() {
        return configManager.getConfig().getInt("roleplay.local-rp-radius", 30);
    }

    public String getMeFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.me-format", "&8* %player% &7%msg%"));
    }

    public String getDoFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.do-format", "&7* %msg%&8: %player%"));
    }

    public String getTrySuccessFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.try.success-format", "&8* %player% &7%msg%... &aУспешно"));
    }

    public String getTryFailureFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.try.failure-format", "&8* %player% &7%msg%... &cНеудачно"));
    }

    public String getTodoFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.todo-format", "&8%player%: &7(%action%) %msg%"));
    }

    public String getGlobalMeFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.global.me-format", "&b! &8* %player% &7%msg%"));
    }

    public String getGlobalDoFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.global.do-format", "&b! &7* %msg%&8: %player%"));
    }

    public String getGlobalTrySuccessFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.global.try.success-format", "&b! &8* %player% &7%msg%... &aУспешно"));
    }

    public String getGlobalTryFailureFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.global.try.failure-format", "&b! &8* %player% &7%msg%... &cНеудачно"));
    }

    public String getGlobalTodoFormat() {
        return ChatUtils.translateColors(configManager.getConfig().getString("roleplay.global.todo-format", "&b! &8%player%: &7(%action%) %msg%"));
    }

    public boolean isBroadcastSoundEnabled() {
        return configManager.getConfig().getBoolean("broadcast.sound.enabled", true);
    }

    public String getBroadcastSoundType() {
        return configManager.getConfig().getString("broadcast.sound.type", "BLOCK_NOTE_BLOCK_PLING");
    }

    public float getBroadcastSoundVolume() {
        return (float) configManager.getConfig().getDouble("broadcast.sound.volume", 1.0);
    }

    public float getBroadcastSoundPitch() {
        return (float) configManager.getConfig().getDouble("broadcast.sound.pitch", 1.0);
    }

    public boolean isLinkFormattingEnabled() {
        return configManager.getConfig().getBoolean("chat.link-formatting.enabled", true);
    }

    public String getLinkColor() {
        return ChatUtils.translateColors(configManager.getConfig().getString("chat.link-formatting.color", "&9&n"));
    }

    public String getLinkHoverText() {
        return configManager.getConfig().getString("chat.link-formatting.hover-text", "&7Нажмите, чтобы открыть ссылку: &f%link%");
    }

    // Эти геттеры могут быть удалены, если ChatFilterManager напрямую обращается к ConfigManager
    // (что, судя по всему, и происходит, т.к. ChatFilterManager получает ConfigManager через ChatPlugin)
    public boolean isAntiSpamEnabled() {
        return configManager.getConfig().getBoolean("chat.anti-spam.enabled", true);
    }

    public int getAntiSpamMessageLimit() {
        return configManager.getConfig().getInt("chat.anti-spam.message-limit", 3);
    }

    @Deprecated
    public long getAntiSpamTimePeriod() {
        return configManager.getConfig().getLong("chat.anti-spam.time-period-seconds", 5);
    }

    public String getAntiSpamKickMessage() {
        return getMessage("error-messages.anti-spam-kick");
    }

    public String getAntiSpamWarningMessage() {
        return getMessage("error-messages.anti-spam-warning");
    }

    public boolean isAntiFloodEnabled() {
        return configManager.getConfig().getBoolean("chat.anti-flood.enabled", true);
    }

    public long getAntiFloodCooldown() {
        return configManager.getConfig().getLong("chat.anti-flood.cooldown-milliseconds", 1000);
    }

    public String getAntiFloodMessage() {
        return getMessage("error-messages.anti-flood");
    }


    /**
     * Возвращает форматированную строку сообщения из конфига, автоматически заменяя %prefix%.
     * Ищет сообщение по нескольким префиксам для удобства.
     * @param path Ключ сообщения в конфиге.
     * @return Переведенное сообщение с подставленным префиксом.
     */
    public String getMessage(String path) {
        String[] prefixes = {
                "error-messages.",
                "command-usages.",
                "private-messages.",
                "ignore.",
                "console-log-formats.",
                "join-quit-messages.",
                "clearchat.",
                ""
        };

        FileConfiguration config = configManager.getConfig();
        if (config == null) {
            getLogger().severe("Конфигурация не загружена при попытке получить сообщение для пути: " + path);
            return ChatColor.RED + "Ошибка: Конфигурация не загружена.";
        }

        String message = null;
        for (String prefix : prefixes) { // Идем по порядку, чтобы найти наиболее подходящий
            if (config.contains(prefix + path)) {
                message = config.getString(prefix + path);
                break;
            }
        }

        if (message == null || message.isEmpty()) {
            getLogger().warning("Сообщение с ключом '" + path + "' не найдено или пусто в config.yml!");
            return ChatColor.RED + "[Сообщение не найдено: " + path + "]";
        }

        // Если это не формат для консольных логов, и сообщение содержит %prefix%, заменяем его
        if (!path.startsWith("console-log-formats.")) {
            message = message.replace("%prefix%", getPluginPrefixTranslated());
        }
        return ChatUtils.translateColors(message);
    }


    public String getPlayerPrefix(UUID playerUUID) {
        return playerDataManager.getPlayerPrefix(playerUUID);
    }

    public String getConsoleLogFormat(String type) {
        return getMessage("console-log-formats." + type);
    }

    // Этот метод sendLocalMessage() теперь не используется, так как
    // вся логика обработки локального чата перенесена в LocalChatListener.
    // Если он нигде больше не используется, его можно безопасно удалить.
    @Deprecated
    public void sendLocalMessage(Player senderPlayer, String message) {
        String chatFormat = getLocalChatFormat();
        int radius = getLocalChatRadius();
        String playerPrefix = getPlayerDataManager().getPlayerPrefix(senderPlayer.getUniqueId());

        String finalMessage = chatFormat
                .replace("{prefix}", ChatUtils.translateColors(playerPrefix))
                .replace("{player}", senderPlayer.getDisplayName())
                .replace("{message}", message);

        Bukkit.getScheduler().runTask(this, () -> {
            List<Player> nearbyPlayers = senderPlayer.getNearbyEntities(radius, radius, radius)
                    .stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .collect(Collectors.toList());

            if (!nearbyPlayers.contains(senderPlayer)) {
                nearbyPlayers.add(senderPlayer);
            }

            for (Player nearbyPlayer : nearbyPlayers) {
                nearbyPlayer.sendMessage(finalMessage);
            }
        });

        getChatLogger().logChatMessage(message, senderPlayer.getName(), "LOCAL", "LOCAL");
    }

    // Метод для получения экземпляра AdventoraBans другими классами
    public AdventoraBans getAdventoraBansPlugin() {
        return adventoraBansPlugin;
    }
}