package org.teverus.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.MessageManager;
import org.teverus.chat.utils.ChatUtils; // Убедитесь, что этот импорт есть, если используется ChatUtils

public class TogglePrivateMessagesCommand implements CommandExecutor {

    private final ChatPlugin plugin;
    private final MessageManager messageManager;

    public TogglePrivateMessagesCommand(ChatPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // ИСПРАВЛЕНО: Используем ключ "player-only"
            sender.sendMessage(plugin.getMessage("player-only")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chat.toggleprivatemessages")) {
            // ИСПРАВЛЕНО: Используем ключ "no-permission"
            player.sendMessage(plugin.getMessage("no-permission")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        boolean currentState = messageManager.isPrivateMessagesEnabled(player);
        messageManager.setPrivateMessagesEnabled(player, !currentState);

        if (!currentState) { // Было выключено, стало включено
            // ИСПРАВЛЕНО: Используем ключ "private-messages-activated"
            player.sendMessage(plugin.getMessage("private-messages-activated")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
        } else { // Было включено, стало выключено
            // ИСПРАВЛЕНО: Используем ключ "private-messages-deactivated"
            player.sendMessage(plugin.getMessage("private-messages-deactivated")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
        }

        return true;
    }
}