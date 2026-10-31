package net.tgc.gamingblock_management.util;

import net.tgc.gamingblock_management.Gamingblock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileManager {
    private Gamingblock plugin;

    Map<String, File> files = new HashMap<>();

    public FileManager(Gamingblock plugin) {
        this.plugin = plugin;
    }

    public void createFile(String name) {
        if (name == null) return;

        plugin.saveResource(name, false);
        files.put(name, new File(plugin.getDataFolder(), name));
        saveFiles();
    }

    public void createFiles(String[] names) {
        if (names == null) return;

        for (String name : names) {
            plugin.saveResource(name, false);

            if (files.containsKey(name)) continue;

            files.put(name, new File(plugin.getDataFolder(), name));
        }

        saveFiles();
    }

    public FileConfiguration getFileData(String fileName) {
        if (fileName == null) return null;
        return YamlConfiguration.loadConfiguration(files.getOrDefault(fileName, null));
    }

    public void saveFileData(String name) {
        if (name == null) return;
        try {
            FileConfiguration data = getFileData(name);
            data.save(getFile(name));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + name + " . Error: " + e.getMessage());
        }
    }

    public File getFile(String fileName) {
        if (fileName == null) return null;
        return files.getOrDefault(fileName, null);
    }

    public FileConfiguration getFileData(File file) {
        if (file == null) return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    public void saveFiles() {
        if (files == null) return;

        for (String file : files.keySet()) {
            plugin.getFileManagerData().set("files." + file + ".name", file);
        }

        plugin.saveFileManagerData();
    }

    public void loadFiles() {
        ConfigurationSection section = plugin.getFileManagerData().getConfigurationSection("files");

        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String fileName = section.getString(key + ".name", null);
            if (fileName == null) continue;

            File file = new File(plugin.getDataFolder(), fileName);
            files.put(fileName, file);
        }
    }
}
