package org.teverus.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.MessageManager;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final ChatPlugin plugin;
    private final MessageManager messageManager;

    public ReplyCommand(ChatPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // ИСПРАВЛЕНО: Правильный ключ из config.yml и убрана лишняя замена %prefix%
            sender.sendMessage(plugin.getMessage("error-messages.player-only")); //
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chat.reply")) {
            // ИСПРАВЛЕНО: Правильный ключ из config.yml и убрана лишняя замена %prefix%
            player.sendMessage(plugin.getMessage("error-messages.no-permission")); //
            return true;
        }

        if (args.length == 0) {
            // ИСПРАВЛЕНО: Правильный ключ из config.yml и убрана лишняя замена %prefix%
            player.sendMessage(plugin.getMessage("command-usages.reply").replace("%command%", label)); //
            return true;
        }

        UUID lastMessagedUUID = messageManager.getLastMessaged(player.getUniqueId());

        if (lastMessagedUUID == null) {
            // ИСПРАВЛЕНО: Правильный ключ из config.yml ("private-messages.no-one-to-reply") и убрана лишняя замена %prefix%
            player.sendMessage(plugin.getMessage("private-messages.no-one-to-reply")); //
            return true;
        }

        Player target = Bukkit.getPlayer(lastMessagedUUID);

        if (target == null || !target.isOnline()) {
            // ИСПРАВЛЕНО: Правильный ключ из config.yml ("private-messages.target-not-online") и убрана лишняя замена %prefix%
            // Это решает проблему "Сообщение не найдено: target_not_online" для /reply.
            player.sendMessage(plugin.getMessage("private-messages.target-not-online")); //
            // НЕ НУЖНО удалять lastMessaged.remove(player.getUniqueId()) здесь, так как MessageManager.reply() уже это делает.
            // lastMessaged.remove(player.getUniqueId());
            return true;
        }

        String message = String.join(" ", args);

        // Эта проверка message.isEmpty() после String.join(" ", args) лишняя,
        // так как args.length == 0 уже обработан выше. Я ее закомментировал, как ты и сделал.
        // if (message.isEmpty()) {
        //     player.sendMessage(plugin.getMessage("reply_usage").replace("%command%", label));
        //     return true;
        // }

        messageManager.sendMessage(player, target, message);
        return true;
    }
}