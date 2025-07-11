package org.teverus.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
// import org.bukkit.entity.Player; // Не нужен, если sender не приводится к Player
import org.teverus.chat.ChatPlugin;
// import org.teverus.chat.utils.ChatUtils; // Не нужен, если не используется напрямую
// import org.bukkit.Sound; // Не нужен, если звук обрабатывается в BroadcastManager

public class BroadcastCommand implements CommandExecutor {

    private final ChatPlugin plugin;

    public BroadcastCommand(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chat.broadcast")) {
            sender.sendMessage(plugin.getMessage("no_permission") // ИЗМЕНЕНО: теперь используется no_permission
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("broadcast_usage") // <-- ЭТО ВЫЗОВ для использования команды
                    .replace("%command%", label)
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        String message = String.join(" ", args);

        // Вызываем BroadcastManager для отправки сообщения и логирования.
        // BroadcastManager.broadcastMessage() УЖЕ СОДЕРЖИТ логирование.
        plugin.getBroadcastManager().broadcastMessage(message);

        return true;
    }
}