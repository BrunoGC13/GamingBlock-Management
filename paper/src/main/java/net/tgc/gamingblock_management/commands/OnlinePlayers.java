package net.tgc.gamingblock_management.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public class OnlinePlayers implements BasicCommand {
    MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getExecutor() instanceof Player p)) {
            return;
        }

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        p.sendMessage(mm.deserialize(onlinePlayers.size() + " players currently online:"));
        Component players = null;

        for (Player online : onlinePlayers) {
            players.append(mm.deserialize(", " + online.getName()));
        }

        if (players != null) {
            p.sendMessage(players);
        }

    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(permission());
    }

    @Override
    public @Nullable String permission() {
        return "gamingblock.commands.online_players";
    }
}