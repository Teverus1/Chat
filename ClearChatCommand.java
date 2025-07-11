package org.teverus.chat.commands; // Убедись, что путь к пакету правильный

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin; // Импортируем твой главный класс плагина

public class ClearChatCommand implements CommandExecutor {

    private final ChatPlugin plugin;

    public ClearChatCommand(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверяем наличие у отправителя команды необходимых прав (permissions)
        if (!sender.hasPermission("chat.clearchat")) {
            // Отправляем сообщение об отсутствии прав, используя твою систему сообщений из конфига
            sender.sendMessage(plugin.getMessage("error-messages.no-permission"));
            return true; // Возвращаем true, так как команда обработана
        }

        // Отправляем 100 пустых строк каждому игроку онлайн, чтобы "очистить" их чат
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) { // Количество строк для очистки
                player.sendMessage(" "); // Отправляем пустую строку (можно использовать "" или " ")
            }
            // Отправляем сообщение игроку, что его чат был очищен (из конфига)
            player.sendMessage(plugin.getMessage("clearchat.cleared-for-player"));
        }

        // Отправляем сообщение отправителю команды (консоли или игроку) о том, что чат очищен (из конфига)
        sender.sendMessage(plugin.getMessage("clearchat.cleared-for-sender"));

        // Логируем действие в файл логов плагина
        plugin.getChatLogger().logAction(sender.getName() + " очистил чат.");

        return true; // Команда успешно обработана
    }
}