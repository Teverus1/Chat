package org.teverus.chat.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.teverus.chat.ChatPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerDataManager {

    private final ChatPlugin plugin;
    private final Map<UUID, String> playerPrefixes = new HashMap<>();
    private final Map<UUID, List<UUID>> ignoredPlayers = new HashMap<>();
    private final Map<UUID, String> customJoinMessages = new HashMap<>(); // Новое поле
    private final Map<UUID, String> customQuitMessages = new HashMap<>(); // Новое поле


    private final File playersFolder;

    public PlayerDataManager(ChatPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "playerdata");
        createPlayersFolder();
        loadAllPlayerData();
    }

    private void createPlayersFolder() {
        if (!playersFolder.exists()) {
            if (playersFolder.mkdirs()) {
                plugin.getLogger().info("Создана папка для данных игроков: " + playersFolder.getAbsolutePath());
            } else {
                plugin.getLogger().severe("Не удалось создать папку для данных игроков: " + playersFolder.getAbsolutePath());
            }
        }
    }

    private File getPlayerFile(UUID playerUUID) {
        return new File(playersFolder, playerUUID.toString() + ".yml");
    }

    public void ensurePlayerDataLoaded(UUID playerUUID) {
        if (playerPrefixes.containsKey(playerUUID) && ignoredPlayers.containsKey(playerUUID)
                && customJoinMessages.containsKey(playerUUID) && customQuitMessages.containsKey(playerUUID)) { // Проверяем и новые поля
            return;
        }

        File playerFile = getPlayerFile(playerUUID);
        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);

        if (!playerFile.exists()) {
            playerPrefixes.put(playerUUID, "");
            ignoredPlayers.put(playerUUID, new ArrayList<>());
            customJoinMessages.put(playerUUID, null); // Дефолтное значение: null
            customQuitMessages.put(playerUUID, null); // Дефолтное значение: null
            savePlayerData(playerUUID);
            plugin.getLogger().info("Созданы и сохранены дефолтные данные для игрока " + playerUUID + ". Файл: " + playerFile.getName());
            return;
        }

        String prefix = playerData.getString("prefix", "");
        playerPrefixes.put(playerUUID, prefix);

        List<String> ignoredUUIDStrings = playerData.getStringList("ignored-players");
        List<UUID> currentIgnored = ignoredUUIDStrings.stream()
                .map(uuidStr -> {
                    try {
                        return UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Некорректный UUID в списке игнорирования игрока " + playerUUID + ": " + uuidStr);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        ignoredPlayers.put(playerUUID, currentIgnored);

        // Загружаем новые поля
        String joinMsg = playerData.getString("custom-join-message"); // null, если не существует
        String quitMsg = playerData.getString("custom-quit-message"); // null, если не существует
        customJoinMessages.put(playerUUID, joinMsg);
        customQuitMessages.put(playerUUID, quitMsg);


        plugin.getLogger().info("Загружены данные игрока " + playerUUID + ": Префикс '" + prefix + "', Игнорируемых: " + currentIgnored.size()
                + ", JoinMsg: " + (joinMsg != null ? "'" + joinMsg + "'" : "отсутствует")
                + ", QuitMsg: " + (quitMsg != null ? "'" + quitMsg + "'" : "отсутствует"));
    }

    public void loadAllPlayerData() {
        plugin.getLogger().info("Начата загрузка всех данных игроков из папки '" + playersFolder.getName() + "'...");
        if (!playersFolder.exists() || !playersFolder.isDirectory()) {
            plugin.getLogger().warning("Папка для данных игроков не существует или не является директорией: " + playersFolder.getAbsolutePath());
            return;
        }

        File[] playerFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null || playerFiles.length == 0) {
            plugin.getLogger().info("В папке '" + playersFolder.getName() + "' не найдено файлов данных игроков.");
            return;
        }

        for (File file : playerFiles) {
            try {
                UUID playerUUID = UUID.fromString(file.getName().replace(".yml", ""));
                ensurePlayerDataLoaded(playerUUID);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректное имя файла данных игрока (не UUID): " + file.getName() + " - " + e.getMessage());
            }
        }
        plugin.getLogger().info("Все данные игроков загружены. Всего загружено: " + playerPrefixes.size() + " игроков.");
    }

    public void savePlayerData(UUID playerUUID) {
        File playerFile = getPlayerFile(playerUUID);
        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);

        playerData.set("prefix", playerPrefixes.getOrDefault(playerUUID, ""));

        List<UUID> currentIgnored = ignoredPlayers.getOrDefault(playerUUID, new ArrayList<>());
        List<String> ignoredUUIDStrings = currentIgnored.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        playerData.set("ignored-players", ignoredUUIDStrings);

        // Сохраняем новые поля
        playerData.set("custom-join-message", customJoinMessages.get(playerUUID));
        playerData.set("custom-quit-message", customQuitMessages.get(playerUUID));

        try {
            playerData.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить данные игрока " + playerUUID + ": " + e.getMessage());
        }
    }

    public void saveAllPlayerData() {
        plugin.getLogger().info("Начато сохранение всех данных игроков...");
        for (UUID playerUUID : new ArrayList<>(playerPrefixes.keySet())) {
            savePlayerData(playerUUID);
        }
        plugin.getLogger().info("Все данные игроков сохранены.");

        // Очищаем кэши после сохранения
        playerPrefixes.clear();
        ignoredPlayers.clear();
        customJoinMessages.clear(); // Очищаем новые кэши
        customQuitMessages.clear(); // Очищаем новые кэши
        plugin.getLogger().info("Кэш данных игроков очищен.");
    }

    public void removePlayerData(UUID playerUUID) {
        playerPrefixes.remove(playerUUID);
        ignoredPlayers.remove(playerUUID);
        customJoinMessages.remove(playerUUID); // Удаляем из кэша новые поля
        customQuitMessages.remove(playerUUID); // Удаляем из кэша новые поля
    }

    @Deprecated
    public void loadPlayerData(UUID playerUUID) {
        ensurePlayerDataLoaded(playerUUID);
    }

    public String getPlayerPrefix(UUID playerUUID) {
        ensurePlayerDataLoaded(playerUUID);
        return playerPrefixes.getOrDefault(playerUUID, "");
    }

    public void setPlayerPrefix(UUID playerUUID, String prefix) {
        playerPrefixes.put(playerUUID, prefix);
        savePlayerData(playerUUID);
    }

    public boolean isIgnoring(UUID ignorerUUID, UUID ignoredUUID) {
        ensurePlayerDataLoaded(ignorerUUID);
        List<UUID> ignorerList = ignoredPlayers.getOrDefault(ignorerUUID, new ArrayList<>());
        return ignorerList.contains(ignoredUUID);
    }

    public void addIgnoredPlayer(UUID ignorerUUID, UUID playerToIgnoreUUID) {
        ensurePlayerDataLoaded(ignorerUUID);
        List<UUID> list = ignoredPlayers.computeIfAbsent(ignorerUUID, k -> new ArrayList<>());
        if (!list.contains(playerToIgnoreUUID)) {
            list.add(playerToIgnoreUUID);
            savePlayerData(ignorerUUID);
        }
    }

    public void removeIgnoredPlayer(UUID ignorerUUID, UUID playerToUnignoreUUID) {
        ensurePlayerDataLoaded(ignorerUUID);
        List<UUID> ignorerList = ignoredPlayers.getOrDefault(ignorerUUID, new ArrayList<>());
        if (ignorerList.remove(playerToUnignoreUUID)) {
            savePlayerData(ignorerUUID);
        }
    }

    public List<UUID> getIgnoredPlayers(UUID playerUUID) {
        ensurePlayerDataLoaded(playerUUID);
        return new ArrayList<>(ignoredPlayers.getOrDefault(playerUUID, new ArrayList<>()));
    }

    // --- Новые методы для пользовательских сообщений входа/выхода ---

    public String getCustomJoinMessage(UUID playerUUID) {
        ensurePlayerDataLoaded(playerUUID);
        return customJoinMessages.get(playerUUID); // Вернет null, если не установлено
    }

    public void setCustomJoinMessage(UUID playerUUID, String message) {
        customJoinMessages.put(playerUUID, message);
        savePlayerData(playerUUID);
    }

    public String getCustomQuitMessage(UUID playerUUID) {
        ensurePlayerDataLoaded(playerUUID);
        return customQuitMessages.get(playerUUID); // Вернет null, если не установлено
    }

    public void setCustomQuitMessage(UUID playerUUID, String message) {
        customQuitMessages.put(playerUUID, message);
        savePlayerData(playerUUID);
    }
}