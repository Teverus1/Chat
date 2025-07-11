package org.teverus.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.PlayerDataManager;

import java.util.UUID;

public class IgnoreCommand implements CommandExecutor {

    private final ChatPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public IgnoreCommand(ChatPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // ИСПОЛЬЗУЕМ error-messages.player-only
            sender.sendMessage(plugin.getMessage("error-messages.player-only")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("chat.ignore")) {
            // ИСПОЛЬЗУЕМ error-messages.no-permission
            player.sendMessage(plugin.getMessage("error-messages.no-permission")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        if (args.length == 0) {
            // ИСПОЛЬЗУЕМ command-usages.ignore
            player.sendMessage(plugin.getMessage("command-usages.ignore")
                    .replace("%command%", label)
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);

        // Внимание: getFirstPlayed() может вернуть 0 для несуществующих игроков.
        // Более надежная проверка - Bukkit.getOfflinePlayer(targetName) и дальнейшая проверка, существует ли профиль.
        // Но для текущей логики, если getFirstPlayed() == 0, считаем игрока не найденным.
        // ИСПОЛЬЗУЕМ error-messages.player-not-found
        if (targetOfflinePlayer == null || targetOfflinePlayer.getFirstPlayed() == 0) {
            player.sendMessage(plugin.getMessage("error-messages.player-not-found")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        UUID targetUUID = targetOfflinePlayer.getUniqueId();

        if (player.getUniqueId().equals(targetUUID)) {
            // ИСПОЛЬЗУЕМ ignore.self-ignore-message
            player.sendMessage(plugin.getMessage("ignore.self-ignore-message")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        if (playerDataManager.isIgnoring(player.getUniqueId(), targetUUID)) {
            // ИСПОЛЬЗУЕМ ignore.already-ignoring-message
            player.sendMessage(plugin.getMessage("ignore.already-ignoring-message")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated())
                    .replace("%targetName%", targetOfflinePlayer.getName()));
            return true;
        }

        playerDataManager.addIgnoredPlayer(player.getUniqueId(), targetUUID);
        // ИСПОЛЬЗУЕМ ignore.ignore-success-message
        player.sendMessage(plugin.getMessage("ignore.ignore-success-message")
                .replace("%prefix%", plugin.getPluginPrefixTranslated())
                .replace("%targetName%", targetOfflinePlayer.getName()));

        plugin.getChatLogger().logChatMessage(targetOfflinePlayer.getName(), player.getName(), null, "IGNORED_PLAYER_ADD");

        return true;
    }
}