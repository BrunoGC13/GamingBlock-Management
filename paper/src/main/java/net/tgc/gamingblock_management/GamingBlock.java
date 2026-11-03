package net.tgc.gamingblock_management;

import net.tgc.gamingblock_management.commands.OnlinePlayers;
import net.tgc.gamingblock_management.commands.PunishmentCommand;
import net.tgc.gamingblock_management.commands.VanishCommand;
import net.tgc.gamingblock_management.db.DatabaseManager;
import net.tgc.gamingblock_management.manager.CommandManager;
import net.tgc.gamingblock_management.manager.VanishManager;
import net.tgc.gamingblock_management.punishments.*;
import net.tgc.gamingblock_management.util.FileManager;
import org.bukkit.Bukkit;
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
    private DatabaseManager databaseManager;
    public String serverName;

    private File fileManagerFile;
    private FileConfiguration fileManagerData;

    @Override
    public void onEnable() {
        fileManagerFile = new File(getDataFolder(), "file_manager.yml");
        fileManagerData = YamlConfiguration.loadConfiguration(fileManagerFile);
        fileManager = new FileManager(this);
        serverName = getConfig().getString("server-name", "<gray>[<blue>Gaming<red>Block<gray>]");

        databaseManager = new DatabaseManager(this);

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

        commandManager.registerCommand(
                "ban",
                "Permanently ban a player",
                new PunishmentCommand(
                        this,
                        PunishmentType.BAN
                )
        );

        commandManager.registerCommand(
                "kick",
                "Kick a player",
                new PunishmentCommand(
                        this,
                        PunishmentType.KICK
                )
        );

        commandManager.registerCommand(
                "tempban",
                "Temporarily ban a player",
                new PunishmentCommand(
                        this,
                        PunishmentType.TEMPBAN
                )
        );

        getLogger().info("GamingBlock Management Plugin loaded!");
    }

    @Override
    public void onDisable() {
        if (punishmentManager != null) {
            punishmentManager.close();
        }

        if (fileManager != null) {
            fileManager.saveFiles();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("GamingBlock Management Plugin disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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

    public boolean isRunningOnVelocity() {
        // Checks if proxies.velocity.enabled is set to true in paper-global.yml
        return Bukkit.getServerConfig().isProxyEnabled();
    }
}