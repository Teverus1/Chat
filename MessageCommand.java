package org.teverus.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.MessageManager;

public class MessageCommand implements CommandExecutor {

    private final ChatPlugin plugin;
    private final MessageManager messageManager;

    public MessageCommand(ChatPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // УДАЛЕНО: Отладочная строка, так как она была помечена как DEBUG и, вероятно, не нужна в продакшене.
        // plugin.getLogger().info("DEBUG: MessageCommand - command label received: " + label);

        if (!(sender instanceof Player)) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "player-only" (находится в error-messages)
            sender.sendMessage(plugin.getMessage("player-only")); // Убрал .replace("%prefix%", ...), так как getMessage уже добавляет префикс
            return true;
        }

        Player player = (Player) sender; // Приведение к типу Player

        if (!player.hasPermission("chat.message")) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "no-permission" (находится в error-messages)
            player.sendMessage(plugin.getMessage("no-permission")); // Убрал .replace("%prefix%", ...), так как getMessage уже добавляет префикс
            return true;
        }

        if (args.length < 2) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "msg" (находится в command-usages)
            player.sendMessage(plugin.getMessage("command-usages.msg").replace("%command%", label)); // getMessage уже добавляет префикс
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            // ИСПРАВЛЕНО: Теперь %targetName% заменяется на args[0] (имя, которое ввел игрок)
            player.sendMessage(plugin.getMessage("private-messages.no-message-received").replace("%targetName%", args[0]));
            return true;
        }

        // Проверка на отправку самому себе
        // ИСПРАВЛЕНО: Используем 'player.getUniqueId()' вместо 'sender.getUniqueId()' (уже было исправлено, оставляю как есть)
        // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "target-is-sender" (находится в error-messages)
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(plugin.getMessage("error-messages.target-is-sender")); // Убрал .replace("%prefix%", ...), так как getMessage уже добавляет префикс
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder(); // Используем StringBuilder для эффективности
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim(); // Убрал += и trim() в цикле

        if (message.isEmpty()) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "msg" (находится в command-usages)
            player.sendMessage(plugin.getMessage("command-usages.msg").replace("%command%", label)); // getMessage уже добавляет префикс
            return true;
        }

        messageManager.sendMessage(player, target, message);
        return true;
    }
}