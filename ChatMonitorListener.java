package org.teverus.chat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.teverus.chat.ChatPlugin;

public class ChatMonitorListener implements Listener {

    private final ChatPlugin plugin;

    public ChatMonitorListener(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    // Слушает событие с наименьшим приоритетом и игнорирует отмененные,
    // чтобы не дублировать логирование сообщений, которые уже обработаны
    // и залогированы другими слушателями нашего плагина (LocalChatListener, GlobalChatListener).
    // Если event.isCancelled() == true, значит, наше событие уже было обработано и залогировано
    // в LocalChatListener или GlobalChatListener.
    // Если вы хотите логировать ВСЕ попытки чата, даже если они были отменены (например, из-за мута),
    // вам нужно изменить ignoreCancelled на 'false'.
    // Но будьте осторожны: это может привести к двойному логированию, если ChatPlugin сам
    // отменяет и логирует события, а потом ChatMonitorListener тоже логирует отмененное.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) // Оставляем 'true' если вы хотите логировать только НЕОТМЕНЕННЫЕ события
    public void onAnyChat(AsyncPlayerChatEvent event) {
        // Если вы хотите логировать только те сообщения, которые НЕ БЫЛИ обработаны
        // LocalChatListener или GlobalChatListener (т.е. не являются ни локальными, ни глобальными,
        // и не были отменены AdventoraBans), то текущая логика с ignoreCancelled = true подходит.
        // В этом случае, если AdventoraBans отменил событие, этот слушатель не сработает.
        // Если GlobalChatListener или LocalChatListener отменили событие, этот слушатель тоже не сработает.

        // Текущая логика логирует сообщения, которые дошли до MONITOR и не были отменены
        // другими слушателями с более высоким приоритетом (включая ваших)
        plugin.getChatLogger().logChatMessage(
                event.getMessage(),
                event.getPlayer().getName(),
                null, // Здесь null, так как тип чата определяется по другим слушателям
                "UNKNOWN" // Тип чата неизвестен для этого слушателя
        );
        plugin.getChatLogger().logChatMessage(
                event.getMessage(),
                event.getPlayer().getName(),
                null, // Здесь null, так как тип чата определяется по другим слушателям
                "MONITOR_CATCH" // Изменил на более конкретный тип для лога
        );
    }
}