package org.teverus.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.ChatUtils;

import java.util.Collection;
import java.util.stream.Collectors;

public class RPCommandExecutor implements CommandExecutor {

    private final ChatPlugin plugin;

    public RPCommandExecutor(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("player-only")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        Player player = (Player) sender;

        // Определяем тип RP-команды
        String rpType = command.getName().toLowerCase(); // me, do, try, todo, gme, gdo, gtry, gtodo

        // Проверка разрешений для RP-команд
        if (!player.hasPermission("chat.rp." + rpType)) {
            sender.sendMessage(plugin.getMessage("no-permission")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        // Проверка на наличие аргументов
        if (args.length == 0 && !rpType.equalsIgnoreCase("try") && !rpType.equalsIgnoreCase("gtry")) {
            // Для /try и /gtry 0 аргументов допустимо (будет случайный результат)
            // Для остальных, если нет аргументов, показываем usage
            if (rpType.equalsIgnoreCase("todo") || rpType.equalsIgnoreCase("gtodo")) {
                sender.sendMessage(plugin.getMessage("rp-todo") // Используем rp-todo usage
                        .replace("%prefix%", plugin.getPluginPrefixTranslated())
                        .replace("%command%", label));
            } else {
                sender.sendMessage(plugin.getMessage("rp-general") // Общий usage для RP команд
                        .replace("%prefix%", plugin.getPluginPrefixTranslated())
                        .replace("%command%", label));
            }
            return true;
        }

        String message = String.join(" ", args);
        String formattedMessage;
        String logType; // Тип для логирования в ChatLogger

        boolean isGlobal = rpType.startsWith("g");
        int radius = isGlobal ? -1 : plugin.getLocalRpRadius(); // -1 для глобального, чтобы не фильтровать по радиусу

        switch (rpType) {
            case "me":
                formattedMessage = plugin.getMeFormat() // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%msg%", message);
                logType = "LOCAL_RP_ME";
                break;
            case "do":
                formattedMessage = plugin.getDoFormat() // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%msg%", message);
                logType = "LOCAL_RP_DO";
                break;
            case "try":
                boolean success = Math.random() < 0.5; // 50% шанс на успех
                formattedMessage = (success ? plugin.getTrySuccessFormat() : plugin.getTryFailureFormat()) // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%msg%", message);
                logType = "LOCAL_RP_TRY";
                break;
            case "todo":
                String[] todoParts = message.split("\\*", 2);
                String action = todoParts.length > 0 ? todoParts[0].trim() : "";
                String todoMessage = todoParts.length > 1 ? todoParts[1].trim() : "";

                formattedMessage = plugin.getTodoFormat() // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%action%", action)
                        .replace("%msg%", todoMessage);
                logType = "LOCAL_RP_TODO";
                break;
            case "gme":
                formattedMessage = plugin.getGlobalMeFormat() // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%msg%", message);
                logType = "GLOBAL_RP_GME";
                break;
            case "gdo":
                formattedMessage = plugin.getGlobalDoFormat() // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%msg%", message);
                logType = "GLOBAL_RP_GDO";
                break;
            case "gtry":
                boolean gSuccess = Math.random() < 0.5;
                formattedMessage = (gSuccess ? plugin.getGlobalTrySuccessFormat() : plugin.getGlobalTryFailureFormat()) // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%msg%", message);
                logType = "GLOBAL_RP_GTRY";
                break;
            case "gtodo":
                String[] gTodoParts = message.split("\\*", 2);
                String gAction = gTodoParts.length > 0 ? gTodoParts[0].trim() : "";
                String gTodoMessage = gTodoParts.length > 1 ? gTodoParts[1].trim() : "";

                formattedMessage = plugin.getGlobalTodoFormat() // ИСПРАВЛЕНО: Использование нового геттера
                        .replace("%player%", player.getDisplayName())
                        .replace("%action%", gAction)
                        .replace("%msg%", gTodoMessage);
                logType = "GLOBAL_RP_GTODO";
                break;
            default:
                // Это не должно произойти, если команды зарегистрированы правильно
                sender.sendMessage(plugin.getMessage("error-messages.unknown-command") // Или более общее сообщение об ошибке
                        .replace("%prefix%", plugin.getPluginPrefixTranslated()));
                return true;
        }

        final String finalFormattedMessage = ChatUtils.translateColors(formattedMessage);

        if (isGlobal) {
            // Отправляем всем онлайн игрокам
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                onlinePlayer.sendMessage(finalFormattedMessage);
            }
        } else {
            // Отправляем игроку и тем, кто находится в радиусе
            Collection<? extends Player> nearbyPlayers = player.getNearbyEntities(radius, radius, radius)
                    .stream()
                    .filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .collect(Collectors.toList());

            // Отправляем сообщение себе
            player.sendMessage(finalFormattedMessage);

            // Отправляем сообщение ближайшим игрокам (исключая себя, если уже отправили)
            for (Player nearbyPlayer : nearbyPlayers) {
                if (!nearbyPlayer.getUniqueId().equals(player.getUniqueId())) {
                    nearbyPlayer.sendMessage(finalFormattedMessage);
                }
            }
        }

        // Логирование в консоль
        plugin.getLogger().info(ChatUtils.translateColors(plugin.getConsoleLogFormat("local-rp") // Используем общий формат для RP логов
                .replace("%player%", player.getName())
                .replace("%message%", message)
                .replace("%command%", label))); // Добавил %command% для RP логов

        // Логирование в файл
        plugin.getChatLogger().logChatMessage(message, player.getName(), null, logType);

        return true;
    }
}