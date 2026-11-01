package net.tgc.gamingblock_management.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.tgc.gamingblock_management.suspicions.Suspicion;
import net.tgc.gamingblock_management.suspicions.SuspicionsManager;
import net.tgc.gamingblock_management.suspicions.SuspicionType;

import java.util.Locale;
import java.util.Optional;

public final class SuspicionCommand {
    private SuspicionCommand() {
    }

    public static BrigadierCommand createBrigadierCommand(
            ProxyServer server,
            SuspicionsManager manager
    ) {
        LiteralArgumentBuilder<CommandSource> root =
                BrigadierCommand.literalArgumentBuilder("suspicion")
                        .requires(source ->
                                source.hasPermission("gamingblock.commands.suspicions"));

        LiteralArgumentBuilder<CommandSource> create =
                BrigadierCommand.literalArgumentBuilder("create")
                        .then(playerArgument("player")
                                .then(typeArgument("type")
                                        .then(BrigadierCommand.requiredArgumentBuilder(
                                                                "reason",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(context -> {
                                                            CommandSource source =
                                                                    context.getSource();

                                                            Optional<Player> issuer =
                                                                    getPlayerSource(source);

                                                            if (issuer.isEmpty()) {
                                                                send(source, "Only players can use this command.",
                                                                        NamedTextColor.RED);
                                                                return 0;
                                                            }

                                                            Player target = findPlayer(
                                                                    server,
                                                                    context.getArgument(
                                                                            "player",
                                                                            String.class
                                                                    )
                                                            );

                                                            if (target == null) {
                                                                send(source, "That player is not online.",
                                                                        NamedTextColor.RED);
                                                                return 0;
                                                            }

                                                            SuspicionType type =
                                                                    parseType(context.getArgument(
                                                                            "type",
                                                                            String.class
                                                                    ));

                                                            if (type == null) {
                                                                sendInvalidType(source);
                                                                return 0;
                                                            }

                                                            String reason =
                                                                    context.getArgument(
                                                                            "reason",
                                                                            String.class
                                                                    );

                                                            Suspicion created =
                                                                    manager.createSuspicion(
                                                                            target.getUniqueId(),
                                                                            target.getUsername(),
                                                                            issuer.get().getUniqueId(),
                                                                            issuer.get().getUsername(),
                                                                            type,
                                                                            reason
                                                                    );

                                                            if (created == null) {
                                                                send(source,
                                                                        "That player already has an active suspicion.",
                                                                        NamedTextColor.RED);
                                                                return 0;
                                                            }

                                                            send(source,
                                                                    "Suspicion created for "
                                                                            + target.getUsername() + ".",
                                                                    NamedTextColor.GREEN);
                                                            return 1;
                                                        })
                                        )
                                )
                        );

        LiteralArgumentBuilder<CommandSource> get =
                BrigadierCommand.literalArgumentBuilder("get")
                        .then(playerArgument("player")
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Player target = findPlayer(
                                            server,
                                            context.getArgument("player", String.class)
                                    );

                                    if (target == null) {
                                        send(source, "That player is not online.",
                                                NamedTextColor.RED);
                                        return 0;
                                    }

                                    sendSuspicion(source,
                                            manager.getSuspicion(target.getUniqueId()));
                                    return 1;
                                })
                        );

        LiteralArgumentBuilder<CommandSource> edit =
                BrigadierCommand.literalArgumentBuilder("edit")
                        .then(playerArgument("player")
                                .then(typeArgument("type")
                                        .then(BrigadierCommand.requiredArgumentBuilder(
                                                                "reason",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(context -> {
                                                            CommandSource source =
                                                                    context.getSource();

                                                            Player target = findPlayer(
                                                                    server,
                                                                    context.getArgument(
                                                                            "player",
                                                                            String.class
                                                                    )
                                                            );

                                                            if (target == null) {
                                                                send(source,
                                                                        "That player is not online.",
                                                                        NamedTextColor.RED);
                                                                return 0;
                                                            }

                                                            SuspicionType type =
                                                                    parseType(context.getArgument(
                                                                            "type",
                                                                            String.class
                                                                    ));

                                                            if (type == null) {
                                                                sendInvalidType(source);
                                                                return 0;
                                                            }

                                                            boolean edited =
                                                                    manager.editSuspicion(
                                                                            target.getUniqueId(),
                                                                            type,
                                                                            context.getArgument(
                                                                                    "reason",
                                                                                    String.class
                                                                            )
                                                                    );

                                                            send(source,
                                                                    edited
                                                                            ? "Suspicion edited successfully."
                                                                            : "That player has no active suspicion.",
                                                                    edited
                                                                            ? NamedTextColor.GREEN
                                                                            : NamedTextColor.RED);
                                                            return edited ? 1 : 0;
                                                        })
                                        )
                                )
                        );

        LiteralArgumentBuilder<CommandSource> remove =
                BrigadierCommand.literalArgumentBuilder("remove")
                        .then(playerArgument("player")
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Player target = findPlayer(
                                            server,
                                            context.getArgument("player", String.class)
                                    );

                                    if (target == null) {
                                        send(source, "That player is not online.",
                                                NamedTextColor.RED);
                                        return 0;
                                    }

                                    boolean removed = manager.removeSuspicion(
                                            target.getUniqueId()
                                    );

                                    send(source,
                                            removed
                                                    ? "Suspicion removed successfully."
                                                    : "That player has no active suspicion.",
                                            removed
                                                    ? NamedTextColor.GREEN
                                                    : NamedTextColor.RED);
                                    return removed ? 1 : 0;
                                })
                        );

        LiteralArgumentBuilder<CommandSource> finish =
                BrigadierCommand.literalArgumentBuilder("finish")
                        .then(playerArgument("player")
                                .executes(context -> {
                                    CommandSource source = context.getSource();
                                    Player target = findPlayer(
                                            server,
                                            context.getArgument("player", String.class)
                                    );

                                    if (target == null) {
                                        send(source, "That player is not online.",
                                                NamedTextColor.RED);
                                        return 0;
                                    }

                                    boolean finished = manager.finishSuspicion(
                                            target.getUniqueId()
                                    );

                                    send(source,
                                            finished
                                                    ? "Suspicion marked as finished."
                                                    : "That player has no active suspicion.",
                                            finished
                                                    ? NamedTextColor.GREEN
                                                    : NamedTextColor.RED);
                                    return finished ? 1 : 0;
                                })
                        );

        root.then(create);
        root.then(get);
        root.then(edit);
        root.then(remove);
        root.then(finish);

        LiteralCommandNode<CommandSource> command = root.build();
        return new BrigadierCommand(command);
    }

    private static RequiredArgumentBuilder<CommandSource, String> playerArgument(
            String name
    ) {
        return BrigadierCommand.requiredArgumentBuilder(
                name,
                StringArgumentType.word()
        );
    }

    private static RequiredArgumentBuilder<CommandSource, String> typeArgument(
            String name
    ) {
        return BrigadierCommand.requiredArgumentBuilder(
                name,
                StringArgumentType.word()
        );
    }

    private static Player findPlayer(ProxyServer server, String name) {
        return server.getPlayer(name).orElse(null);
    }

    private static Optional<Player> getPlayerSource(CommandSource source) {
        if (source instanceof Player player) {
            return Optional.of(player);
        }

        return Optional.empty();
    }

    private static SuspicionType parseType(String value) {
        try {
            return SuspicionType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void sendSuspicion(
            CommandSource source,
            Suspicion suspicion
    ) {
        if (suspicion == null) {
            send(source, "That player has no active suspicion.",
                    NamedTextColor.YELLOW);
            return;
        }

        send(source, "Suspicion information:", NamedTextColor.GOLD);
        send(source, "Target: " + suspicion.getTargetName(),
                NamedTextColor.GRAY);
        send(source, "Issuer: " + suspicion.getIssuerName(),
                NamedTextColor.GRAY);
        send(source, "Type: " + suspicion.getType().name(),
                NamedTextColor.GRAY);
        send(source, "Reason: " + suspicion.getReason(),
                NamedTextColor.GRAY);
        send(source, "Finished: " + suspicion.isFinished(),
                NamedTextColor.GRAY);
    }

    private static void sendInvalidType(CommandSource source) {
        send(source,
                "Invalid type. Use: BAN, TEMPBAN, KICK, or WARN.",
                NamedTextColor.RED);
    }

    private static void send(
            CommandSource source,
            String message,
            NamedTextColor color
    ) {
        source.sendMessage(Component.text(message, color));
    }
}