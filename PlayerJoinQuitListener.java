package org.teverus.chat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority; // Для контроля порядка обработки событий
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.teverus.chat.ChatPlugin;

public class PlayerJoinQuitListener implements Listener {

    private final ChatPlugin plugin;

    public PlayerJoinQuitListener(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    // Приоритет HIGH - это нормальный приоритет для изменения сообщения входа/выхода.
    // Если вам нужно, чтобы это срабатывало РАНЬШЕ других плагинов, используйте HIGHEST.
    // Если позже, то NORMAL или LOW.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Убедитесь, что PlayerDataManager обрабатывает данные асинхронно или очень быстро,
        // чтобы не блокировать основной поток сервера при входе игрока.
        plugin.getPlayerDataManager().ensurePlayerDataLoaded(event.getPlayer().getUniqueId());

        // Получаем кастомное сообщение входа и устанавливаем его
        String customJoinMessage = plugin.getJoinQuitMessageManager().getPlayerJoinMessage(event.getPlayer().getUniqueId());
        if (customJoinMessage != null && !customJoinMessage.isEmpty()) {
            event.setJoinMessage(customJoinMessage);
        } else {
            // Если кастомного нет, используем дефолтное из конфига плагина
            String defaultJoinMessage = plugin.getMessage("join-quit-messages.default-join-message")
                    .replace("{player}", event.getPlayer().getDisplayName());
            event.setJoinMessage(defaultJoinMessage);
        }
    }

    @EventHandler(priority = EventPriority.HIGH) // Устанавливаем высокий приоритет
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Убедитесь, что PlayerDataManager обрабатывает данные асинхронно или очень быстро.
        plugin.getPlayerDataManager().savePlayerData(event.getPlayer().getUniqueId());
        plugin.getPlayerDataManager().removePlayerData(event.getPlayer().getUniqueId());

        // Получаем кастомное сообщение выхода и устанавливаем его
        String customQuitMessage = plugin.getJoinQuitMessageManager().getPlayerQuitMessage(event.getPlayer().getUniqueId());
        if (customQuitMessage != null && !customQuitMessage.isEmpty()) {
            event.setQuitMessage(customQuitMessage);
        } else {
            // Если кастомного нет, используем дефолтное из конфига плагина
            String defaultQuitMessage = plugin.getMessage("join-quit-messages.default-quit-message")
                    .replace("{player}", event.getPlayer().getDisplayName());
            event.setQuitMessage(defaultQuitMessage);
        }
    }
}