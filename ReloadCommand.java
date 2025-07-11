package org.teverus.chat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.ChatUtils;

public class ReloadCommand implements CommandExecutor {

    private final ChatPlugin plugin;

    public ReloadCommand(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chat.reload")) {
            sender.sendMessage(plugin.getMessage("no_permission"));
            return true;
        }

        // Перезагрузка конфигурации
        plugin.reloadConfig();
        // Переинициализация менеджеров, которые зависят от конфига
        // Например, ChatLogger должен быть переинициализирован, чтобы использовать новый файл,
        // но здесь мы просто перезапустим его через onDisable/onEnable при перезагрузке плагина
        // или если вы хотите, можно добавить метод в ChatLogger для перезагрузки
        // plugin.getChatLogger().reloadLogger(); // Если бы такой метод был

        // Для полной перезагрузки всех зависимостей, лучше использовать /reload или команду плагина
        // которая вызовет onDisable() и onEnable().
        // Но для конфига достаточно reloadConfig().
        sender.sendMessage(ChatUtils.translateColors(plugin.getPluginPrefix() + " &aКонфигурация плагина перезагружена."));

        // Если вы хотите, чтобы лог файл архивировался при каждой перезагрузке конфига,
        // вы можете вызвать методы ChatLogger здесь:
        plugin.getChatLogger().archiveLogFile(); // Архивировать текущий лог
        plugin.getChatLogger().createNewLogFile(); // Создать новый лог
        return true;
    }
}