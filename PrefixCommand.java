package org.teverus.chat.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.teverus.chat.ChatPlugin;
import org.teverus.chat.utils.ChatUtils;
import org.teverus.chat.utils.PlayerDataManager;

import java.util.Arrays; // Добавлено для работы с подмассивами аргументов
import java.util.UUID; // Добавлен для корректной работы с UUID OfflinePlayer

public class PrefixCommand implements CommandExecutor {

    private final ChatPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public PrefixCommand(ChatPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Использование для установки своего префикса или очистки
            if (sender instanceof Player) {
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-self" (находится в command-usages)
                sender.sendMessage(plugin.getMessage("prefix-self") // Изменено с "prefix_usage_self"
                        .replace("%command%", label)
                        .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            } else {
                // Если консоль вводит без аргументов, предложить usage для других
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-other" (находится в command-usages)
                sender.sendMessage(plugin.getMessage("prefix-other") // Изменено с "prefix_usage_other"
                        .replace("%command%", label)
                        .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            }
            return true;
        }

        String targetName;
        String prefixInput;
        Player playerSender = null; // Переименовал, чтобы не путать с 'player' внутри if/else блоков

        // Определяем, устанавливаем ли мы префикс себе или другому игроку
        boolean isAdminCommand = false;
        if (args.length >= 2 && sender.hasPermission("chat.prefix.other")) {
            // Команда: /prefix <ник> <префикс> (для админа)
            targetName = args[0];
            prefixInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length)); // Собираем остаток как префикс
            isAdminCommand = true;
        } else if (sender instanceof Player) {
            // Команда: /prefix <префикс> (для себя)
            playerSender = (Player) sender;
            targetName = playerSender.getName(); // Цель - сам игрок
            prefixInput = String.join(" ", args); // Весь ввод - это префикс
        } else {
            // Консоль пытается установить префикс себе или недостаточно аргументов для другого
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-other" (находится в command-usages)
            sender.sendMessage(plugin.getMessage("prefix-other") // Изменено с "prefix_usage_other"
                    .replace("%command%", label)
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        // Проверка разрешений для установки префикса другому игроку
        // Если это была админская команда, и отправитель не имеет прав
        if (isAdminCommand && !sender.hasPermission("chat.prefix.other")) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "no-permission" (находится в error-messages)
            sender.sendMessage(plugin.getMessage("no-permission") // Изменено с "no_permission"
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }
        // Дополнительная проверка: Если игрок пытается установить префикс другому БЕЗ прав админа
        // (т.е. он ввел "/prefix <имя_другого_игрока> <префикс>", но он не админ)
        if (!isAdminCommand && playerSender != null && !targetName.equalsIgnoreCase(playerSender.getName())) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-self" (находится в command-usages)
            sender.sendMessage(plugin.getMessage("prefix-self") // Правильное usage для команды /prefix <префикс>
                    .replace("%command%", label)
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }


        // Получаем объект OfflinePlayer (даже если игрок оффлайн)
        OfflinePlayer targetOfflinePlayer = Bukkit.getOfflinePlayer(targetName);

        // Более надежная проверка на существование игрока:
        // Используем hasPlayedBefore() или просто проверяем, был ли игрок онлайн.
        // getOfflinePlayer всегда возвращает объект, даже для несуществующего имени.
        // isOnline() проверяет, онлайн ли игрок СЕЙЧАС.
        // hasPlayedBefore() проверяет, был ли игрок когда-либо на сервере и имеет ли UUID.
        // Если игрок никогда не играл и не онлайн, считаем его "не найденным".
        if (!targetOfflinePlayer.hasPlayedBefore() && !targetOfflinePlayer.isOnline()) {
            // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "player-not-found" (находится в error-messages)
            sender.sendMessage(plugin.getMessage("player-not-found") // Изменено с "player_not_found"
                    .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            return true;
        }

        // Получаем объект Player, если целевой игрок онлайн
        Player targetOnlinePlayer = targetOfflinePlayer.isOnline() ? targetOfflinePlayer.getPlayer() : null;


        if (prefixInput.equalsIgnoreCase("clear")) {
            plugin.getPlayerDataManager().setPlayerPrefix(targetOfflinePlayer.getUniqueId(), "");
            if (isAdminCommand) {
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-cleared-admin" (находится в error-messages)
                sender.sendMessage(plugin.getMessage("prefix-cleared-admin") // Изменено с "prefix_cleared_admin"
                        .replace("%prefix%", plugin.getPluginPrefixTranslated())
                        .replace("%targetName%", targetOfflinePlayer.getName()));
                if (targetOnlinePlayer != null) {
                    // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-cleared-target-notify" (находится в error-messages)
                    targetOnlinePlayer.sendMessage(plugin.getMessage("prefix-cleared-target-notify") // Изменено с "prefix_cleared_target"
                            .replace("%prefix%", plugin.getPluginPrefixTranslated()));
                }
            } else { // Если игрок очищает свой собственный префикс
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-cleared-self" (находится в error-messages)
                sender.sendMessage(plugin.getMessage("prefix-cleared-self") // Новое сообщение для себя
                        .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            }
        } else {
            String strippedPrefix = ChatUtils.stripColor(prefixInput);
            if (strippedPrefix.length() > 16) {
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-too-long" (находится в error-messages)
                sender.sendMessage(plugin.getMessage("prefix-too-long") // Изменено с "prefix_too_long"
                        .replace("%prefix%", plugin.getPluginPrefixTranslated()));
                return true;
            }

            String formattedPrefix = ChatUtils.translateColors(prefixInput);
            // Добавляем пробел, только если префикс не пуст и не заканчивается пробелом или цветовым кодом
            // Regex `.*§.$` проверяет, заканчивается ли строка на § (цветовой код) и любой символ после него
            if (!formattedPrefix.isEmpty() && !formattedPrefix.endsWith(" ") && !formattedPrefix.matches(".*§.$")) {
                formattedPrefix += " ";
            }
            plugin.getPlayerDataManager().setPlayerPrefix(targetOfflinePlayer.getUniqueId(), formattedPrefix);

            if (isAdminCommand) {
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-set-admin" (находится в error-messages)
                sender.sendMessage(plugin.getMessage("prefix-set-admin") // Изменено с "prefix_set_admin"
                        .replace("%prefix_value%", formattedPrefix)
                        .replace("%prefix%", plugin.getPluginPrefixTranslated())
                        .replace("%targetName%", targetOfflinePlayer.getName()));
                if (targetOnlinePlayer != null) {
                    // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-set-target-notify" (находится в error-messages)
                    targetOnlinePlayer.sendMessage(plugin.getMessage("prefix-set-target-notify") // Изменено с "prefix_set_target"
                            .replace("%prefix_value%", formattedPrefix)
                            .replace("%prefix%", plugin.getPluginPrefixTranslated())
                            .replace("%senderName%", sender.getName()));
                }
            } else { // Если игрок устанавливает свой собственный префикс
                // ИСПРАВЛЕНО: Ключ сообщения в config.yml - "prefix-set-self" (находится в error-messages)
                sender.sendMessage(plugin.getMessage("prefix-set-self") // Новое сообщение для себя
                        .replace("%prefix_value%", formattedPrefix)
                        .replace("%prefix%", plugin.getPluginPrefixTranslated()));
            }
        }
        return true;
    }
}