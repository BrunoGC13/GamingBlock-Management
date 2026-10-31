package net.tgc.gamingblock_management;

import net.tgc.gamingblock_management.commands.OnlinePlayers;
import net.tgc.gamingblock_management.manager.CommandManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Gamingblock extends JavaPlugin {
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        commandManager = new CommandManager(this);

        commandManager.registerCommand("onlineplayers", List.of("olist", "oplayers", "players"), "Shows online players", new OnlinePlayers());

        getLogger().info("GamingBlock Management Plugin loaded!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GamingBlock Management Plugin disabled!");
    }
}