package net.tgc.gamingblock_management;

import net.tgc.gamingblock_management.commands.OnlinePlayers;
import net.tgc.gamingblock_management.commands.SuspectionCommand;
import net.tgc.gamingblock_management.manager.CommandManager;
import net.tgc.gamingblock_management.suspections.SuspectionsManager;
import net.tgc.gamingblock_management.util.FileManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class Gamingblock extends JavaPlugin {
    private CommandManager commandManager;
    private FileManager fileManager;
    private SuspectionsManager suspectionsManager;

    private File fileManagerFile;
    private FileConfiguration fileManagerData;

    @Override
    public void onEnable() {
        fileManagerFile = new File(getDataFolder(), "file_manager.yml");
        fileManagerData = YamlConfiguration.loadConfiguration(fileManagerFile);
        fileManager = new FileManager(this);
        fileManager.loadFiles();

        String[] fileNames = {
                "suspections.yml"
        };
        fileManager.createFiles(fileNames);

        suspectionsManager = new SuspectionsManager(this);
        suspectionsManager.loadSuspections();

        commandManager = new CommandManager(this);

        commandManager.registerCommand("onlineplayers", List.of("olist", "oplayers", "players"), "Shows online players", new OnlinePlayers());
        commandManager.registerCommand(
                "suspection",
                List.of("suspections", "suspect"),
                "Manages player suspections",
                new SuspectionCommand(this)
        );

        getLogger().info("GamingBlock Management Plugin loaded!");
    }

    @Override
    public void onDisable() {
        fileManager.saveFiles();
        getLogger().info("GamingBlock Management Plugin disabled!");
    }

    public SuspectionsManager getSuspectionsManager() {
        return suspectionsManager;
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