package net.tgc.gamingblock_management;

import net.tgc.gamingblock_management.commands.OnlinePlayers;
import net.tgc.gamingblock_management.commands.VanishCommand;
import net.tgc.gamingblock_management.manager.CommandManager;
import net.tgc.gamingblock_management.manager.VanishManager;
import net.tgc.gamingblock_management.punishments.PunishmentLayout;
import net.tgc.gamingblock_management.punishments.PunishmentLayoutRegistry;
import net.tgc.gamingblock_management.punishments.PunishmentListener;
import net.tgc.gamingblock_management.punishments.PunishmentManager;
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
    private VanishManager vanishManager;
    private PunishmentManager punishmentManager;
    private PunishmentLayoutRegistry punishmentLayoutRegistry;
    public String serverName;

    private File fileManagerFile;
    private FileConfiguration fileManagerData;

    @Override
    public void onEnable() {
        fileManagerFile = new File(getDataFolder(), "file_manager.yml");
        fileManagerData = YamlConfiguration.loadConfiguration(fileManagerFile);
        fileManager = new FileManager(this);
        serverName = getConfig().getString("server-name", "<gray>[<blue>Gaming<red>Block<gray>]");

        String[] files = {
                "punishments.yml",
                "layouts.yml"
        };

        fileManager.createFiles(files);
        fileManager.loadFiles();

        punishmentLayoutRegistry = new PunishmentLayoutRegistry(this);
        punishmentLayoutRegistry.loadAll();

        punishmentManager = new PunishmentManager(this);
        getServer().getPluginManager().registerEvents(
                new PunishmentListener(this),
                this
        );

        vanishManager = new VanishManager(this);
        getServer().getPluginManager().registerEvents(vanishManager, this);

        commandManager = new CommandManager(this);
        commandManager.registerCommand(
                "onlineplayers",
                List.of("olist", "oplayers", "players"),
                "Shows online players",
                new OnlinePlayers()
        );

        commandManager.registerCommand(
                "vanish",
                List.of("v"),
                "Set vanish mode",
                new VanishCommand(this)
        );

        getLogger().info("GamingBlock Management Plugin loaded!");
    }

    @Override
    public void onDisable() {
        fileManager.saveFiles();
        getLogger().info("GamingBlock Management Plugin disabled!");
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public PunishmentLayoutRegistry getPunishmentLayoutRegistry() {
        return punishmentLayoutRegistry;
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
            getLogger().severe(
                    "Could not save file_manager.yml! Error: "
                            + e.getMessage()
            );
        }
    }

    public String getServerName() {
        return serverName;
    }
}