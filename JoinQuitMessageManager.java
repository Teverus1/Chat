package org.teverus.chat.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;

import java.util.UUID;

public class JoinQuitMessageManager {

    private final ChatPlugin plugin;
    private final PlayerDataManager playerDataManager;

    // Кэшированные значения настроек
    private boolean enabled;
    private int maxMessageLength;

    public JoinQuitMessageManager(ChatPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        loadSettings(); // Загружаем настройки при инициализации
    }

    /**
     * Загружает настройки из конфигурации плагина.
     */
    public void loadSettings() {
        // Используем getConfigManager().getConfig() для доступа к конфигу
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("join-quit-messages.enabled", true);
        this.maxMessageLength = plugin.getConfigManager().getConfig().getInt("join-quit-messages.max-message-length", 60);
        plugin.getLogger().info("Настройки JoinQuitMessageManager перезагружены.");
    }

    /**
     * Перезагружает настройки для JoinQuitMessageManager.
     * Этот метод будет вызываться из ChatPlugin.reloadConfig().
     */
    public void reloadConfigSettings() {
        loadSettings();
    }

    public String getPlayerJoinMessage(UUID playerUUID) {
        if (!enabled) { // Используем кэшированное значение
            return null; // Если система отключена, возвращаем null, чтобы использовалось дефолтное сообщение Spigot
        }
        String customMessage = playerDataManager.getCustomJoinMessage(playerUUID);
        if (customMessage == null || customMessage.isEmpty()) {
            return null; // Если у игрока нет кастомного сообщения, возвращаем null
        }
        // Заменяем плейсхолдер {player} на display name игрока
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null) {
            return ChatUtils.translateColors(customMessage.replace("{player}", player.getDisplayName()));
        }
        return ChatUtils.translateColors(customMessage); // Если игрок не онлайн, просто переводим цвета
    }

    public String getPlayerQuitMessage(UUID playerUUID) {
        if (!enabled) { // Используем кэшированное значение
            return null; // Если система отключена, возвращаем null
        }
        String customMessage = playerDataManager.getCustomQuitMessage(playerUUID);
        if (customMessage == null || customMessage.isEmpty()) {
            return null; // Если у игрока нет кастомного сообщения, возвращаем null
        }
        // Заменяем плейсхолдер {player} на display name игрока
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null) {
            return ChatUtils.translateColors(customMessage.replace("{player}", player.getDisplayName()));
        }
        return ChatUtils.translateColors(customMessage); // Если игрок не онлайн, просто переводим цвета
    }

    public void setPlayerJoinMessage(UUID playerUUID, String message) {
        playerDataManager.setCustomJoinMessage(playerUUID, message);
    }

    public void setPlayerQuitMessage(UUID playerUUID, String message) {
        playerDataManager.setCustomQuitMessage(playerUUID, message);
    }

    public int getMaxMessageLength() {
        return maxMessageLength; // Используем кэшированное значение
    }
}