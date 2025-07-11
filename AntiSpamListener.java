package org.teverus.chat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.ChatFilterManager;
import org.teverus.chat.utils.ChatUtils;

import java.util.HashMap; // Добавил, если Map не импортировался автоматически
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamListener implements Listener {

    private final ChatPlugin plugin;
    private final ChatFilterManager chatFilterManager;
    private final Map<UUID, Long> lastChatMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> messageCount = new ConcurrentHashMap<>(); // Для спама, если используется счетчик

    public AntiSpamListener(ChatPlugin plugin, ChatFilterManager chatFilterManager) {
        this.plugin = plugin;
        this.chatFilterManager = chatFilterManager;
    }

    // Приоритет HIGH: Срабатывает после AdventoraBans (HIGHEST), но до LocalChatListener/GlobalChatListener (NORMAL).
    @EventHandler(priority = EventPriority.HIGH)
    public void onChatEvent(AsyncPlayerChatEvent event) {
        // --- ВАЖНОЕ ИЗМЕНЕНИЕ: ПРОВЕРКА НА ОТМЕНУ СОБЫТИЯ ---
        // Если AdventoraBans уже отменил событие (из-за мута), то AntiSpamListener не должен ничего делать.
        if (event.isCancelled()) {
            return;
        }
        // --- КОНЕЦ ДОБАВЛЕНИЯ ---

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String message = event.getMessage();

        // Проверка на анти-флуд
        if (chatFilterManager.isAntiFloodEnabled()) {
            long lastMessage = lastChatMessageTime.getOrDefault(playerUUID, 0L);
            long cooldown = chatFilterManager.getAntiFloodCooldown();

            if (System.currentTimeMillis() - lastMessage < cooldown) {
                event.setCancelled(true); // Отменяем событие
                player.sendMessage(ChatUtils.translateColors(plugin.getAntiFloodMessage()));
                plugin.getChatLogger().logChatMessage("BLOCKED (Anti-Flood)", player.getName(), "ALL", message); // Логируем блокировку
                return; // Завершаем обработку, так как сообщение заблокировано
            }
            lastChatMessageTime.put(playerUUID, System.currentTimeMillis());
        }

        // Проверка на анти-спам (повторяющиеся сообщения)
        if (chatFilterManager.isAntiSpamEnabled()) {
            // ВАЖНО: ChatFilterManager.isSpamMessage() уже инкрементирует внутренний счетчик
            // и проверяет лимит. Если оно возвращает true, значит это спам.
            if (chatFilterManager.isSpamMessage(playerUUID, message)) {
                // Если isSpamMessage вернул true, это значит, что превышен лимит
                // повторяющихся сообщений или сообщений за период (в зависимости от вашей реализации isSpamMessage).
                event.setCancelled(true); // Отменяем событие
                player.sendMessage(ChatUtils.translateColors(plugin.getAntiSpamWarningMessage()));
                plugin.getChatLogger().logChatMessage("BLOCKED (Anti-Spam)", player.getName(), "ALL", message); // Логируем блокировку
                return; // Завершаем обработку
            }
            // Здесь НЕТ else, так как сброс счетчика должен происходить ВНУТРИ ChatFilterManager.isSpamMessage()
            // или его вспомогательных методов, чтобы корректно отслеживать повторяющиеся сообщения.
        }

        // Проверка на фильтр слов (цензура)
        // Этот фильтр должен применяться только если сообщение не было отменено ранее.
        if (chatFilterManager.isWordFilterEnabled()) {
            String filteredMessage = chatFilterManager.filterMessage(message);
            if (!filteredMessage.equals(message)) {
                // Если сообщение было отфильтровано (содержало запрещенные слова)
                event.setMessage(filteredMessage); // Изменяем сообщение в событии
                // Опционально: уведомить игрока о цензуре
                player.sendMessage(ChatUtils.translateColors(plugin.getMessage("error-messages.censored-message")));
                plugin.getChatLogger().logChatMessage("CENSORED", player.getName(), "ALL", message + " -> " + filteredMessage); // Логируем цензуру
                // Здесь не нужно event.setCancelled(true), если вы хотите, чтобы отфильтрованное сообщение
                // все равно прошло в чат. Если вы хотите блокировать сообщения с цензурой, добавьте:
                // event.setCancelled(true);
                // return;
            }
        }
    }
}