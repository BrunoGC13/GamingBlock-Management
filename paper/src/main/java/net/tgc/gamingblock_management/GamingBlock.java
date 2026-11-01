package net.tgc.gamingblock_management;

import net.tgc.gamingblock_management.commands.OnlinePlayers;
import net.tgc.gamingblock_management.manager.CommandManager;
import net.tgc.gamingblock_management.util.FileManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class GamingBlock extends JavaPlugin {
    private CommandManager commandManager;
    private FileManager fileManager;

    private File fileManagerFile;
    private FileConfiguration fileManagerData;

    @Override
    public void onEnable() {
        fileManagerFile = new File(getDataFolder(), "file_manager.yml");
        fileManagerData = YamlConfiguration.loadConfiguration(fileManagerFile);
        fileManager = new FileManager(this);
        fileManager.loadFiles();

        commandManager = new CommandManager(this);

        commandManager.registerCommand("onlineplayers", List.of("olist", "oplayers", "players"), "Shows online players", new OnlinePlayers());

        getLogger().info("GamingBlock Management Plugin loaded!");
    }

    @Override
    public void onDisable() {
        fileManager.saveFiles();
        getLogger().info("GamingBlock Management Plugin disabled!");
    }

    public FileConfiguration getFileManagerData() {
        return fileManagerData;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public void saveFileManagerData() {
        try {
            fileManagerData.save(fileManagerFile);
        } catch (IOException e) {
            getLogger().severe("Could not save file_manager.yml! Error: " + e.getMessage());
        }
    }
}