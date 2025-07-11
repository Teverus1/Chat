package org.teverus.chat.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.ChatUtils;
import org.teverus.chat.utils.ChatLogger;
import org.teverus.chat.utils.ChatFilterManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.BaseComponent;

public class GlobalChatListener implements Listener {

    private final ChatPlugin plugin;
    private final ChatLogger chatLogger;
    private final ChatFilterManager chatFilterManager;

    // Регулярное выражение для поиска ссылок.
    private final Pattern urlPattern = Pattern.compile("((https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}(/\\S*)?)", Pattern.CASE_INSENSITIVE);

    public GlobalChatListener(ChatPlugin plugin) {
        this.plugin = plugin;
        this.chatLogger = plugin.getChatLogger();
        this.chatFilterManager = plugin.getChatFilterManager();
    }

    // Устанавливаем приоритет MONITOR, чтобы этот слушатель запускался после других (например, AdventoraBans на HIGHEST).
    // Это гарантирует, что мьюты, установленные другими плагинами, будут работать.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        // Если событие уже отменено (например, из-за мута в AdventoraBans или антиспама),
        // то этот слушатель не будет обрабатывать сообщение, тем самым соблюдая мут.
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Проверяем, является ли это глобальным чатом (сообщение начинается с "!" и у игрока есть пермишен).
        boolean isGlobalChat = message.startsWith("!") && player.hasPermission("chat.global");

        if (!isGlobalChat) {
            // Если это не глобальный чат, выходим. LocalChatListener обработает его.
            return;
        }

        // --- ВАЖНО ДЛЯ ИНТЕГРАЦИИ С DISCORDSRV ---
        // Если мы отменяем событие (event.setCancelled(true)), DiscordSRV,
        // который обычно слушает события чата, может не получить это сообщение,
        // так как он может игнорировать отмененные события.

        // Поскольку мы используем BungeeCord TextComponents для форматирования ссылок и rich text,
        // мы вынуждены отменить событие, чтобы предотвратить стандартное сообщение Bukkit и
        // отправить форматированное сообщение вручную.
        event.setCancelled(true);

        String actualMessage = message.substring(1); // Удаляем "!"

        String playerPrefix = plugin.getPlayerDataManager().getPlayerPrefix(player.getUniqueId());
        String globalChatFormat = plugin.getGlobalChatFormat();

        // Готовим сообщение игрока с учетом пермишенов на цвета
        String processedMessage = ChatUtils.translateColorsWithPermissions(player, actualMessage);

        // 1. Формируем полную строку для парсинга
        String fullFormattedMessage = globalChatFormat
                .replace("{prefix}", ChatUtils.translateColors(playerPrefix.isEmpty() ? "" : playerPrefix))
                .replace("{player}", player.getDisplayName())
                .replace("{message}", processedMessage);

        // 2. Используем TextComponent.fromLegacyText() для парсинга всей строки.
        BaseComponent[] components = TextComponent.fromLegacyText(fullFormattedMessage);

        // 3. Собираем конечный TextComponent, обрабатывая ссылки
        TextComponent finalChatComponent = new TextComponent();
        if (plugin.isLinkFormattingEnabled()) {
            String linkColor = plugin.getLinkColor();
            String rawLinkHoverText = plugin.getLinkHoverText();

            for (BaseComponent component : components) {
                TextComponent currentTextComponent = (component instanceof TextComponent) ? (TextComponent) component : new TextComponent(component.toLegacyText());

                String componentText = currentTextComponent.getText();
                if (componentText == null || componentText.isEmpty()) {
                    finalChatComponent.addExtra(currentTextComponent);
                    continue;
                }

                Matcher matcher = urlPattern.matcher(componentText);
                int lastEnd = 0;
                while (matcher.find()) {
                    String beforeLink = componentText.substring(lastEnd, matcher.start());
                    if (!beforeLink.isEmpty()) {
                        TextComponent textPart = new TextComponent(beforeLink);
                        textPart.copyFormatting(currentTextComponent);
                        finalChatComponent.addExtra(textPart);
                    }

                    String url = matcher.group(1);
                    String fullUrl = url;
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        fullUrl = "http://" + url;
                    }

                    TextComponent linkComponent = new TextComponent(linkColor + url);
                    linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, fullUrl));
                    linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(ChatUtils.translateColors(rawLinkHoverText.replace("%link%", url))))));

                    finalChatComponent.addExtra(linkComponent);
                    lastEnd = matcher.end();
                }
                String afterLinks = componentText.substring(lastEnd);
                if (!afterLinks.isEmpty()) {
                    TextComponent textPart = new TextComponent(afterLinks);
                    textPart.copyFormatting(currentTextComponent);
                    finalChatComponent.addExtra(textPart);
                }
            }
        } else {
            for (BaseComponent component : components) {
                finalChatComponent.addExtra(component);
            }
        }

        // Рассылаем сообщение всем игрокам онлайн
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!plugin.getPlayerDataManager().isIgnoring(onlinePlayer.getUniqueId(), player.getUniqueId())) {
                onlinePlayer.spigot().sendMessage(finalChatComponent);
            }
        }

        // Логируем сообщение
        chatLogger.logChatMessage(actualMessage, player.getName(), null, "GLOBAL");
    }
}