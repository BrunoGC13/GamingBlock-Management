package net.tgc.gamingblock_management.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.tgc.gamingblock_management.Gamingblock;
import net.tgc.gamingblock_management.suspections.Suspection;
import net.tgc.gamingblock_management.suspections.SuspectionType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SuspectionCommand implements BasicCommand {
    private final Gamingblock plugin;

    public SuspectionCommand(Gamingblock plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getExecutor() instanceof Player issuer)) {
            source.getSender().sendMessage(
                    Component.text("Only players can use this command.", NamedTextColor.RED)
            );
            return;
        }

        if (args.length == 0) {
            sendUsage(issuer);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "create" -> create(issuer, args);
            case "get", "show" -> get(issuer, args);
            case "edit" -> edit(issuer, args);
            case "remove", "delete" -> remove(issuer, args);
            case "finish", "complete" -> finish(issuer, args);
            default -> sendUsage(issuer);
        }
    }

    private void create(Player issuer, String[] args) {
        if (args.length < 4) {
            issuer.sendMessage(Component.text(
                    "Usage: /suspection create <player> <type> <reason>",
                    NamedTextColor.RED
            ));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        SuspectionType type = parseType(args[2]);

        if (target == null) {
            issuer.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return;
        }

        if (type == null) {
            sendInvalidType(issuer);
            return;
        }

        String reason = joinArguments(args, 3);

        Suspection suspection = plugin.getSuspectionsManager().createSuspection(
                target,
                issuer,
                type,
                reason
        );

        if (suspection == null) {
            issuer.sendMessage(Component.text(
                    "That player already has an active suspicion.",
                    NamedTextColor.RED
            ));
            return;
        }

        issuer.sendMessage(Component.text(
                "Suspicion created for " + target.getName() + ".",
                NamedTextColor.GREEN
        ));
    }

    private void get(Player issuer, String[] args) {
        if (args.length < 2) {
            issuer.sendMessage(Component.text(
                    "Usage: /suspection get <player>",
                    NamedTextColor.RED
            ));
            return;
        }

        Suspection suspection = plugin.getSuspectionsManager().getSuspection(args[1]);

        if (suspection == null) {
            issuer.sendMessage(Component.text(
                    "That player has no active suspicion.",
                    NamedTextColor.YELLOW
            ));
            return;
        }

        issuer.sendMessage(Component.text("Suspicion information:", NamedTextColor.GOLD));
        issuer.sendMessage(Component.text(
                "Target: " + suspection.getTarget().getName(),
                NamedTextColor.GRAY
        ));
        issuer.sendMessage(Component.text(
                "Issuer: " + suspection.getIssuer().getName(),
                NamedTextColor.GRAY
        ));
        issuer.sendMessage(Component.text(
                "Type: " + suspection.getType().name(),
                NamedTextColor.GRAY
        ));
        issuer.sendMessage(Component.text(
                "Reason: " + suspection.getReason(),
                NamedTextColor.GRAY
        ));
        issuer.sendMessage(Component.text(
                "Finished: " + suspection.isFinished(),
                NamedTextColor.GRAY
        ));
    }

    private void edit(Player issuer, String[] args) {
        if (args.length < 4) {
            issuer.sendMessage(Component.text(
                    "Usage: /suspection edit <player> <type> <reason>",
                    NamedTextColor.RED
            ));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        SuspectionType type = parseType(args[2]);

        if (target == null) {
            issuer.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return;
        }

        if (type == null) {
            sendInvalidType(issuer);
            return;
        }

        boolean edited = plugin.getSuspectionsManager().editSuspection(
                target,
                type,
                joinArguments(args, 3)
        );

        issuer.sendMessage(Component.text(
                edited
                        ? "Suspicion edited successfully."
                        : "That player has no active suspicion.",
                edited ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }

    private void remove(Player issuer, String[] args) {
        if (args.length < 2) {
            issuer.sendMessage(Component.text(
                    "Usage: /suspection remove <player>",
                    NamedTextColor.RED
            ));
            return;
        }

        boolean removed = plugin.getSuspectionsManager().removeSuspection(args[1]);

        issuer.sendMessage(Component.text(
                removed
                        ? "Suspicion removed successfully."
                        : "That player has no active suspicion.",
                removed ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }

    private void finish(Player issuer, String[] args) {
        if (args.length < 2) {
            issuer.sendMessage(Component.text(
                    "Usage: /suspection finish <player>",
                    NamedTextColor.RED
            ));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            issuer.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return;
        }

        boolean finished = plugin.getSuspectionsManager().finishSuspection(target);

        issuer.sendMessage(Component.text(
                finished
                        ? "Suspicion marked as finished."
                        : "That player has no active suspicion.",
                finished ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            return filter(
                    List.of("create", "get", "edit", "remove", "finish"),
                    args[0]
            );
        }

        if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ROOT);

            if (action.equals("create")
                    || action.equals("get")
                    || action.equals("edit")
                    || action.equals("remove")
                    || action.equals("finish")) {
                return filter(
                        Bukkit.getOnlinePlayers()
                                .stream()
                                .map(Player::getName)
                                .toList(),
                        args[1]
                );
            }
        }

        if (args.length == 3
                && (args[0].equalsIgnoreCase("create")
                || args[0].equalsIgnoreCase("edit"))) {
            return filter(
                    Arrays.stream(SuspectionType.values())
                            .map(Enum::name)
                            .toList(),
                    args[2]
            );
        }

        return List.of();
    }

    private List<String> filter(Collection<String> values, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);

        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .toList();
    }

    private SuspectionType parseType(String value) {
        try {
            return SuspectionType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String joinArguments(String[] args, int startIndex) {
        StringBuilder reason = new StringBuilder();

        for (int index = startIndex; index < args.length; index++) {
            if (reason.length() > 0) {
                reason.append(' ');
            }

            reason.append(args[index]);
        }

        return reason.toString();
    }

    private void sendInvalidType(Player player) {
        player.sendMessage(Component.text(
                "Invalid type. Use: BAN, TEMPBAN, KICK, or WARN.",
                NamedTextColor.RED
        ));
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text(
                "/suspection create <player> <type> <reason>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text(
                "/suspection get <player>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text(
                "/suspection edit <player> <type> <reason>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text(
                "/suspection remove <player>",
                NamedTextColor.YELLOW
        ));
        player.sendMessage(Component.text(
                "/suspection finish <player>",
                NamedTextColor.YELLOW
        ));
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(permission());
    }

    @Override
    public @Nullable String permission() {
        return "gamingblock.commands.suspection";
    }
}