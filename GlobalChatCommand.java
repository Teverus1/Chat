package org.teverus.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
// ChatUtils больше не нужен здесь напрямую для форматирования, т.к. это будет делать GlobalChatListener
// import org.teverus.chat.utils.ChatUtils;

public class GlobalChatCommand implements CommandExecutor {

    private final ChatPlugin plugin;

    public GlobalChatCommand(ChatPlugin plugin) {
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

        if (!player.hasPermission("chat.global")) {
            player.sendMessage(plugin.getMessage("no-permission")
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        if (args.length == 0) {
            // Предполагается, что "global_usage" - это ключ для usage в config.yml
            player.sendMessage(plugin.getMessage("global_usage")
                    .replace("%command%", label)
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        String message = String.join(" ", args);

        // --- ВАЖНОЕ ИЗМЕНЕНИЕ: Отправляем сообщение через Bukkit API
        // Это позволит плагину AdventoraBans, AntiSpamListener и GlobalChatListener
        // перехватить событие и обработать его.
        // Добавляем "!" в начало сообщения, чтобы GlobalChatListener знал, что это глобальный чат.
        player.chat("!" + message);

        // Все строки логирования и ручного форматирования удалены из этого класса,
        // так как они теперь будут обрабатываться GlobalChatListener.

        return true;
    }
}