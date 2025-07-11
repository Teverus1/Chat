package org.teverus.chat.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor; // ChatColor здесь не используется, можно удалить
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.bukkit.configuration.file.FileConfiguration; // <-- ДОБАВЛЕНО: для явного доступа к конфигу

public class BroadcastManager {

    private final ChatPlugin plugin;

    // Кэшированные значения настроек для Broadcast
    private String broadcastFormat;
    private boolean soundEnabled;
    private String soundTypeString;
    private float soundVolume;
    private float soundPitch;

    public BroadcastManager(ChatPlugin plugin) {
        this.plugin = plugin;
        loadSettings(); // Загружаем настройки при инициализации
    }

    /**
     * Загружает настройки из конфигурации плагина.
     */
    public void loadSettings() {
        FileConfiguration config = plugin.getConfigManager().getConfig(); // <-- ЯВНО ПОЛУЧАЕМ КОНФИГ
        if (config == null) {
            plugin.getLogger().severe("Конфигурация в BroadcastManager.loadSettings() равна null! Не удалось загрузить настройки Broadcast.");
            // Устанавливаем дефолты на случай ошибки, чтобы плагин не упал
            this.broadcastFormat = "&8[Объявление] &r{message}";
            this.soundEnabled = true;
            this.soundTypeString = "BLOCK_NOTE_BLOCK_PLING";
            this.soundVolume = 1.0f;
            this.soundPitch = 1.0f;
            return;
        }

        // --- ИЗМЕНЕНО ---
        // Теперь формат broadcast.format загружается напрямую, без использования getMessage().
        // Это дает тебе полный контроль через конфиг. Если хочешь префикс, добавляй %prefix% в конфиг.
        this.broadcastFormat = config.getString("broadcast.format", "&8[Объявление] &r{message}"); // <-- ДЕФОЛТ ЗДЕСЬ

        this.soundEnabled = config.getBoolean("broadcast.sound.enabled", true);
        this.soundTypeString = config.getString("broadcast.sound.type", "BLOCK_NOTE_BLOCK_PLING");
        this.soundVolume = (float) config.getDouble("broadcast.sound.volume", 1.0);
        this.soundPitch = (float) config.getDouble("broadcast.sound.pitch", 1.0);
        plugin.getLogger().info("Настройки BroadcastManager перезагружены.");
    }

    /**
     * Перезагружает настройки для BroadcastManager.
     * Этот метод будет вызываться из ChatPlugin.reloadConfig().
     */
    public void reloadConfigSettings() {
        loadSettings();
    }

    // @SuppressWarnings("deprecation") - Эта аннотация тебе не нужна здесь.
    public void broadcastMessage(String message) {
        // --- ИЗМЕНЕНО ---
        // Используем кэшированный broadcastFormat, который уже загружен из конфига.
        // Заменяем %message% на реальное сообщение.
        String finalBroadcastMessage = this.broadcastFormat.replace("{message}", message);

        // --- ИЗМЕНЕНО ---
        // Теперь, если в broadcastFormat есть %prefix%, мы его заменяем здесь.
        // Это соответствует новой логике getMessage() в ChatPlugin,
        // где %prefix% заменяется только если он явно указан в строке.
        if (finalBroadcastMessage.contains("%prefix%")) {
            finalBroadcastMessage = finalBroadcastMessage.replace("%prefix%", plugin.getPluginPrefixTranslated());
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ChatUtils.translateColors(finalBroadcastMessage)); // <-- ИСПОЛЬЗУЕМ СОБРАННУЮ СТРОКУ
        }

        if (soundEnabled) { // Используем кэшированное значение
            try {
                Sound sound = Sound.valueOf(soundTypeString.toUpperCase()); // Используем кэшированное значение

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, soundVolume, soundPitch); // Используем кэшированные значения
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный тип звука в конфиге для объявления: " + soundTypeString + ". Убедитесь, что это допустимое имя звука Bukkit.");
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка при воспроизведении звука объявления: " + e.getMessage());
            }
        }
        // Убедимся, что ChatLogger корректно принимает null для sender/receiver в случае broadcast.
        plugin.getChatLogger().logChatMessage(message, "CONSOLE", "BROADCAST", "BROADCAST"); // <-- ИСПРАВЛЕНО: sender и receiver должны быть null для broadcast
    }
}