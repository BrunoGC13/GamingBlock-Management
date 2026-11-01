package net.tgc.gamingblock_management.manager;

import io.papermc.paper.command.brigadier.BasicCommand;
import net.tgc.gamingblock_management.Gamingblock;

import java.util.Collection;

public class CommandManager {
    Gamingblock plugin;

    public CommandManager(Gamingblock plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(String name, BasicCommand cmd) {
        plugin.registerCommand(name, cmd);
    }

    public void registerCommand(String name, Collection<String> aliases, BasicCommand cmd) {
        plugin.registerCommand(name, aliases, cmd);
    }

    public void registerCommand(String name, String description, BasicCommand cmd) {
        plugin.registerCommand(name, description, cmd);
    }

    public void registerCommand(String name, Collection<String> aliases, String description, BasicCommand cmd) {
        plugin.registerCommand(name, description, aliases, cmd);
    }
}