package org.teverus.chat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.teverus.chat.utils.ChatUtils; // Убедитесь, что этот импорт есть
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;

import net.md_5.bungee.api.chat.BaseComponent;

// Удалены импорты AdventoraBans, так как он больше не будет напрямую проверять муты здесь.
// import org.teverus.adventoraBans.AdventoraBans;
// import org.teverus.adventoraBans.punishments.BanRecord;
// import org.teverus.adventoraBans.punishments.PunishmentType;
// import org.teverus.adventoraBans.util.TimeUtil;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Optional;
// import java.util.UUID;
// import java.util.concurrent.ExecutionException;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.TimeoutException;
// import java.util.logging.Level;


public class LocalChatListener implements Listener {

    private final ChatPlugin plugin;
    // Удалена ссылка на AdventoraBans, так как она не нужна здесь.
    // private final AdventoraBans adventoraBans;
    private final Pattern urlPattern = Pattern.compile("((https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}(/\\S*)?)", Pattern.CASE_INSENSITIVE);


    public LocalChatListener(ChatPlugin plugin) {
        this.plugin = plugin;
        // Удалено получение экземпляра AdventoraBans.
        // this.adventoraBans = (AdventoraBans) plugin.getServer().getPluginManager().getPlugin("AdventoraBans");

        // Удалено предупреждение о ненахождении AdventoraBans.
        // if (this.adventoraBans == null) {
        //     plugin.getLogger().warning("Плагин AdventoraBans не найден! Функционал проверки мутов для чата будет отключен.");
        // }
    }

    @EventHandler(priority = EventPriority.MONITOR) // Priority NORMAL - сработает после HIGHEST (AdventoraBans) и HIGH (AntiSpamListener)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // --- ВАЖНО: ЭТА ПРОВЕРКА ДОЛЖНА БЫТЬ ПЕРВОЙ И ЕДИНСТВЕННОЙ НА ОТМЕНУ СОБЫТИЯ В ЭТОМ ЛИСТЕНЕРЕ ---
        if (event.isCancelled()) {
            // Если событие уже отменено (например, AdventoraBans из-за мута, или AntiSpamListener из-за флуда/спама),
            // то этот слушатель НИЧЕГО НЕ ДЕЛАЕТ и просто выходит.
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();
        // UUID playerUuid = player.getUniqueId(); // Больше не нужен, так как муты проверяются в AdventoraBans

        // --- УДАЛЕН ВЕСЬ БЛОК ПРОВЕРКИ МУТА С ИСПОЛЬЗОВАНИЕМ AdventoraBans API ---
        // Эта логика полностью дублирует и мешает работе AdventoraBans.
        // AdventoraBans должен сам отменять событие и отправлять сообщение игроку.
        // Если AdventoraBans отменил, то event.isCancelled() уже будет true.
        // if (adventoraBans != null && plugin.getConfigManager().isMuteSystemEnabled()) { ... }
        // --- КОНЕЦ УДАЛЕНИЯ БЛОКА МУТА ---


        // Если сообщение начинается с "!", то оно предназначено для глобального чата и должно
        // быть обработано GlobalChatListener, поэтому LocalChatListener должен его игнорировать.
        if (message.startsWith("!")) {
            return;
        }

        // ОТМЕНЯЕМ СОБЫТИЕ, чтобы Bukkit не рассылал сообщение по своему дефолтному формату.
        // Мы будем рассылать его вручную (ниже), чтобы контролировать получателей (локальный радиус)
        // и формат (BungeeCord TextComponent).
        event.setCancelled(true);

        String chatFormat = plugin.getLocalChatFormat(); // Формат из конфига, уже с §-кодами
        String playerPrefix = plugin.getPlayerDataManager().getPlayerPrefix(player.getUniqueId());

        // Готовим сообщение игрока:
        // ИЗМЕНЕНИЕ ЗДЕСЬ: используем новый метод с проверкой пермишенов
        String processedMessage = ChatUtils.translateColorsWithPermissions(player, message);
        // Старая логика удалена, так как она дублирует проверки, которые теперь в ChatUtils
        // if (player.hasPermission("chat.color")) {
        //     processedMessage = ChatUtils.translateColors(message);
        // } else {
        //     processedMessage = ChatUtils.stripColor(message);
        // }


        // 1. Формируем полную строку, которая будет разобрана TextComponent.fromLegacyText().
        // Важно: prefix тоже может содержать цветовые коды, поэтому его переводим отдельно, но без пермишенов игрока
        // так как префикс это часть форматирования сервера/плагина, а не текст игрока.
        String fullFormattedMessage = chatFormat
                .replace("{prefix}", ChatUtils.translateColors(playerPrefix.isEmpty() ? "" : playerPrefix))
                .replace("{player}", player.getDisplayName())
                .replace("{message}", processedMessage); // processedMessage уже с примененными/не примененными цветами

        // 2. Используем TextComponent.fromLegacyText() для парсинга всей строки.
        // Этот метод автоматически переводит §-коды в BungeeCord-формат.
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
                    // Важно: Text.fromLegacyText() здесь используется для корректного форматирования текста при наведении
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

        // Отправляем локальное сообщение игрокам в радиусе (асинхронно)
        final int radius = plugin.getLocalChatRadius();
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getChatLogger().logChatMessage(message, player.getName(), "LOCAL", "LOCAL");

            for (Player recipient : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(recipient.getWorld()) &&
                        player.getLocation().distance(recipient.getLocation()) <= radius) {
                    if (!plugin.getPlayerDataManager().isIgnoring(recipient.getUniqueId(), player.getUniqueId())) {
                        recipient.spigot().sendMessage(finalChatComponent);
                    }
                }
            }
        });
    }
}