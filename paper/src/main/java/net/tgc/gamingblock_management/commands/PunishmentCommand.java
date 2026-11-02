package net.tgc.gamingblock_management.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.tgc.gamingblock_management.GamingBlock;
import net.tgc.gamingblock_management.punishments.DurationParser;
import net.tgc.gamingblock_management.punishments.Punishment;
import net.tgc.gamingblock_management.punishments.PunishmentLayout;
import net.tgc.gamingblock_management.punishments.PunishmentResult;
import net.tgc.gamingblock_management.punishments.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class PunishmentCommand implements BasicCommand {
    private static final String DEFAULT_LAYOUT = "default";

    private final GamingBlock plugin;
    private final PunishmentType type;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PunishmentCommand(
            GamingBlock plugin,
            PunishmentType type
    ) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public void execute(
            CommandSourceStack source,
            String[] args
    ) {
        CommandSender sender = source.getSender();

        int minimumArguments = type == PunishmentType.TEMPBAN ? 2 : 1;

        if (args.length < minimumArguments) {
            sendUsage(sender);
            return;
        }

        OfflinePlayer target = findTarget(args[0]);

        if (target == null) {
            sender.sendMessage(
                    miniMessage.deserialize(
                            "<red>That player could not be found."
                    )
            );
            return;
        }

        int reasonStartIndex;
        Date endsOn = null;

        if (type == PunishmentType.TEMPBAN) {
            try {
                Duration duration = DurationParser.parse(args[1]);

                if (duration.isZero() || duration.isNegative()) {
                    throw new IllegalArgumentException(
                            "Duration must be greater than zero."
                    );
                }

                endsOn = Date.from(Instant.now().plus(duration));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(
                        miniMessage.deserialize(
                                "<red>" + exception.getMessage()
                        )
                );
                return;
            }

            reasonStartIndex = 2;
        } else {
            reasonStartIndex = 1;
        }

        String reason = joinArguments(args, reasonStartIndex);
        PunishmentLayout layout = getDefaultLayout();

        Punishment punishment;

        if (type == PunishmentType.TEMPBAN) {
            punishment = new Punishment(
                    target,
                    getOperator(sender),
                    type,
                    reason,
                    layout,
                    endsOn
            );
        } else {
            punishment = new Punishment(
                    target,
                    getOperator(sender),
                    type,
                    reason,
                    layout
            );
        }

        PunishmentResult result =
                plugin.getPunishmentManager().punish(punishment);

        if (!result.isSuccessful()) {
            sender.sendMessage(
                    miniMessage.deserialize(
                            "<red>Could not apply the punishment."
                    )
            );
            return;
        }

        sender.sendMessage(
                miniMessage.deserialize(
                        "<green>Applied <white>"
                                + type.name().toLowerCase(Locale.ROOT)
                                + " <green>to <white>"
                                + target.getName()
                                + "<green>."
                )
        );
    }

    private OfflinePlayer findTarget(String name) {
        Player onlinePlayer = Bukkit.getPlayerExact(name);

        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayer(name);

        if (!cachedPlayer.hasPlayedBefore()) {
            return null;
        }

        return cachedPlayer;
    }

    private Player getOperator(CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    private PunishmentLayout getDefaultLayout() {
        return plugin.getPunishmentLayoutRegistry()
                .get(DEFAULT_LAYOUT)
                .orElse(null);
    }

    private String joinArguments(
            String[] args,
            int startIndex
    ) {
        if (args.length <= startIndex) {
            return "No reason provided";
        }

        return String.join(
                " ",
                Stream.of(args)
                        .skip(startIndex)
                        .toList()
        );
    }

    private void sendUsage(CommandSender sender) {
        String usage = switch (type) {
            case BAN -> "<yellow>Usage: /ban <player> [reason]";
            case KICK -> "<yellow>Usage: /kick <player> [reason]";
            case TEMPBAN -> "<yellow>Usage: /tempban <player> <duration> [reason]";
            default -> "<red>Invalid punishment command.";
        };

        sender.sendMessage(miniMessage.deserialize(usage));
    }

    @Override
    public Collection<String> suggest(
            CommandSourceStack source,
            String[] args
    ) {
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ROOT);

            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(name ->
                            name.toLowerCase(Locale.ROOT)
                                    .startsWith(current)
                    )
                    .toList();
        }

        if (type == PunishmentType.TEMPBAN && args.length == 2) {
            return List.of(
                    "30s",
                    "10m",
                    "1h",
                    "1d",
                    "7d",
                    "1w"
            );
        }

        return List.of();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission(permission());
    }

    @Override
    public @Nullable String permission() {
        return switch (type) {
            case BAN -> "gamingblock.punishments.ban";
            case KICK -> "gamingblock.punishments.kick";
            case TEMPBAN -> "gamingblock.punishments.tempban";
            default -> "gamingblock.punishments.use";
        };
    }
}