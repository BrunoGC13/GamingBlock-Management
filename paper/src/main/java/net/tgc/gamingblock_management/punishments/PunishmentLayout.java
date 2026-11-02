package net.tgc.gamingblock_management.punishments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.tgc.gamingblock_management.GamingBlock;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class PunishmentLayout {
    private final String name;
    private final String[] messages;
    private Punishment punishment;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PunishmentLayout(
            String name,
            String[] messages,
            Punishment punishment
    ) {
        this.name = name;
        this.messages = messages;
        this.punishment = punishment;
    }

    public PunishmentLayout(
            String name,
            String[] messages
    ) {
        this.name = name;
        this.messages = messages;
        this.punishment = null;
    }

    public void setPunishment(Punishment punishment) {
        this.punishment = punishment;
    }

    public String getName() {
        return name;
    }

    public Component render(String serverName) {
        if (punishment == null) {
            return Component.empty();
        }

        return render(
                punishment.getOperator() == null
                        ? "CONSOLE"
                        : punishment.getOperator().getName(),
                punishment.getReason(),
                punishment.getType(),
                punishment.getEndsOn() == null
                        ? null
                        : punishment.getEndsOn().getTime(),
                serverName
        );
    }

    public Component render(PunishmentEntry entry, String serverName) {
        return render(
                entry.getOperatorName(),
                entry.getReason(),
                entry.getType(),
                entry.getEndsAt() == null
                        ? null
                        : entry.getEndsAt().toEpochMilli(),
                serverName
        );
    }

    private Component render(
            String operatorName,
            String reason,
            PunishmentType type,
            Long endsAt,
            String serverName
    ) {
        Component message = Component.empty();

        for (int index = 0; index < messages.length; index++) {
            String template = messages[index];
            if (index > 0) {
                message = message.append(miniMessage.deserialize("<newline>"));
            }
            String text = template
                    .replace("%OPERATOR%", operatorName == null ? "CONSOLE" : operatorName)
                    .replace("%REASON%", reason == null ? "" : reason)
                    .replace("%TYPE%", type.name().toLowerCase())
                    .replace("%SERVER_NAME%", serverName == null ? "" : serverName);

            if (endsAt == null) {
                text = text.replace("%ENDS_ON%", "Never");
            } else {
                text = text.replace("%ENDS_ON%", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(endsAt)));
            }

            message = message.append(miniMessage.deserialize(text));
        }

        return message;
    }
}
