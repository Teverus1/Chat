package org.teverus.chat.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.teverus.chat.ChatPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final ChatPlugin plugin;
    private FileConfiguration config = null;
    private File configFile = null;

    public ConfigManager(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Настраивает config.yml. Создает его, если он не существует, и загружает.
     * Также копирует отсутствующие дефолтные значения из JAR-файла плагина.
     */
    public void setupConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false); // Копируем дефолтный конфиг из JAR
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Смотрим, есть ли дефолтный конфиг внутри JAR
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig); // Устанавливаем дефолтные значения
            config.options().copyDefaults(true); // Копируем дефолтные значения, если они отсутствуют
            saveConfig(); // Сохраняем, чтобы новые дефолты появились в файле на диске
        }
    }

    /**
     * Получает текущий объект FileConfiguration.
     * Если конфиг еще не загружен, вызывает setupConfig().
     *
     * @return FileConfiguration объекта.
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            setupConfig(); // Убедимся, что конфиг загружен
        }
        return config;
    }

    /**
     * Сохраняет текущий FileConfiguration в файл.
     */
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить конфиг в " + configFile + ": " + ex.getMessage());
        }
    }

    /**
     * Перезагружает FileConfiguration из файла.
     * Также обновляет дефолтные значения из JAR-файла плагина.
     */
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
            config.options().copyDefaults(true);
            saveConfig(); // Сохраняем, чтобы новые дефолты появились в файле на диске
        }
        plugin.getLogger().info("Конфигурация перезагружена ConfigManager.");
    }

    /**
     * Проверяет, включена ли интеграция с системой мутов.
     *
     * @return true, если интеграция с системой мутов включена, иначе false.
     */
    public boolean isMuteSystemEnabled() {
        // Получаем значение из конфига. По умолчанию false, если не указано.
        return getConfig().getBoolean("mute-system-integration.enabled", false);
    }
}