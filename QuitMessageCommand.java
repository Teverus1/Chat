package org.teverus.chat.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.ChatUtils;
import org.teverus.chat.utils.JoinQuitMessageManager;

public class QuitMessageCommand implements CommandExecutor {

    private final ChatPlugin plugin;
    private final JoinQuitMessageManager joinQuitMessageManager;

    public QuitMessageCommand(ChatPlugin plugin, JoinQuitMessageManager joinQuitMessageManager) {
        this.plugin = plugin;
        this.joinQuitMessageManager = joinQuitMessageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("error-messages.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chatplugin.command.quitmessage")) {
            player.sendMessage(plugin.getMessage("error-messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            // Теперь getMessage() сам подставляет префикс
            player.sendMessage(plugin.getMessage("join-quit-messages.usage-quitmessage")
                    .replace("%command%", command.getName()));
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            joinQuitMessageManager.setPlayerQuitMessage(player.getUniqueId(), null);
            // Теперь getMessage() сам подставляет префикс
            player.sendMessage(plugin.getMessage("join-quit-messages.clear-quit-success"));
            return true;
        }

        String message = String.join(" ", args);
        String strippedMessage = ChatColor.stripColor(ChatUtils.translateColors(message));

        if (strippedMessage.length() > joinQuitMessageManager.getMaxMessageLength()) {
            // Теперь getMessage() сам подставляет префикс
            player.sendMessage(plugin.getMessage("join-quit-messages.message-too-long")
                    .replace("%length%", String.valueOf(joinQuitMessageManager.getMaxMessageLength())));
            return true;
        }

        joinQuitMessageManager.setPlayerQuitMessage(player.getUniqueId(), message);
        // Теперь getMessage() сам подставляет префикс
        player.sendMessage(plugin.getMessage("join-quit-messages.set-quit-success")
                .replace("%message%", ChatUtils.translateColors(message)));
        return true;
    }
}