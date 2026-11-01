package net.tgc.gamingblock_management.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class VanishCommand implements BasicCommand {
    private GamingBlock plugin;
    MiniMessage mm = MiniMessage.miniMessage();
    private final String adminPerm = "gamingblock.vanish.admin";

    public VanishCommand(GamingBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getExecutor() instanceof Player p)) return;

        if (args.length == 0) {
            if (plugin.getVanishManager().setVanish(p)) {
                p.sendMessage(mm.deserialize("<green>Enabled vanish."));
            } else {
                p.sendMessage(mm.deserialize("<red>Disabled vanish."));
            }
        } else {
            if (!p.hasPermission(adminPerm)) {
                if (plugin.getVanishManager().setVanish(p)) {
                    p.sendMessage(mm.deserialize("<green>Enabled vanish."));
                } else {
                    p.sendMessage(mm.deserialize("<red>Disabled vanish."));
                }
                return;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                p.sendMessage(mm.deserialize("<red>That player does not exist!"));
                return;
            }

            if (plugin.getVanishManager().setVanish(target)) {
                p.sendMessage(mm.deserialize("<green>Enabled vanish for " + target.getName() + "."));
                target.sendMessage(mm.deserialize("<green>Vanish has been enabled by " + p.getName() + "."));
            } else {
                p.sendMessage(mm.deserialize("<red>Disabled vanish for " + target.getName() + "."));
                target.sendMessage(mm.deserialize("<green>Vanish has been enabled by " + p.getName() + "."));
            }
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (!source.getSender().hasPermission(adminPerm)) return List.of();

        if (args.length == 0) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(currentArg))
                .toList();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(permission());
    }

    @Override
    public @Nullable String permission() {
        return "gamingblock.vanish.use";
    }
}
