package net.tgc.gamingblock_management.punishments;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PunishmentEntry {
    private final UUID targetUuid;
    private final String targetName;
    private final PunishmentType type;
    private final String operatorName;
    private final String reason;
    private final Instant createdAt;
    private final Instant endsAt;
    private final String layout;

    public PunishmentEntry(
            UUID targetUuid,
            String targetName,
            PunishmentType type,
            String operatorName,
            String reason,
            Instant createdAt,
            Instant endsAt,
            String layout
    ) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
        this.operatorName = operatorName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.endsAt = endsAt;
        this.layout = layout;
    }

    public static PunishmentEntry fromPunishment(Punishment punishment) {
        String operatorName = punishment.getOperator() == null
                ? "CONSOLE"
                : punishment.getOperator().getName();

        String layoutName = punishment.getLayout() == null
                ? null
                : punishment.getLayout().getName();

        Instant endsAt = punishment.getEndsOn() == null
                ? null
                : punishment.getEndsOn().toInstant();

        return new PunishmentEntry(
                punishment.getTarget().getUniqueId(),
                punishment.getTarget().getName(),
                punishment.getType(),
                operatorName,
                punishment.getReason(),
                Instant.now(),
                endsAt,
                layoutName
        );
    }

    public static PunishmentEntry unban(
            UUID targetUuid,
            String targetName,
            String operatorName,
            String reason
    ) {
        return new PunishmentEntry(
                targetUuid,
                targetName,
                PunishmentType.UNBAN,
                operatorName,
                reason,
                Instant.now(),
                null,
                null
        );
    }

    public static PunishmentEntry fromMap(Map<?, ?> map) {
        UUID targetUuid = UUID.fromString(
                String.valueOf(map.get("targetUuid"))
        );

        String targetName = getString(
                map,
                "targetName",
                "Unknown"
        );

        PunishmentType type = PunishmentType.valueOf(
                String.valueOf(map.get("type")).toUpperCase()
        );

        String operatorName = getString(
                map,
                "operatorName",
                "CONSOLE"
        );

        String reason = getString(
                map,
                "reason",
                ""
        );

        Instant createdAt = Instant.parse(
                String.valueOf(map.get("createdAt"))
        );

        Object endsAtValue = map.get("endsAt");
        Instant endsAt = endsAtValue == null
                ? null
                : Instant.parse(String.valueOf(endsAtValue));

        Object layoutValue = map.get("layout");
        String layout = layoutValue == null
                ? null
                : String.valueOf(layoutValue);

        return new PunishmentEntry(
                targetUuid,
                targetName,
                type,
                operatorName,
                reason,
                createdAt,
                endsAt,
                layout
        );
    }

    private static String getString(
            Map<?, ?> map,
            String key,
            String defaultValue
    ) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return defaultValue;
        }

        return String.valueOf(map.get(key));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("targetUuid", targetUuid.toString());
        map.put("targetName", targetName);
        map.put("type", type.name());
        map.put("operatorName", operatorName);
        map.put("reason", reason);
        map.put("createdAt", createdAt.toString());
        map.put("endsAt", endsAt == null ? null : endsAt.toString());
        map.put("layout", layout);

        return map;
    }

    public boolean isActiveBan() {
        if (type == PunishmentType.BAN) {
            return true;
        }

        return type == PunishmentType.TEMPBAN
                && endsAt != null
                && Instant.now().isBefore(endsAt);
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public String getLayout() {
        return layout;
    }
}