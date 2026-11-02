package net.tgc.gamingblock_management.punishments;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

public class PunishmentManager {
    private static final String FILE_NAME = "punishments.yml";

    private final GamingBlock plugin;
    private final FileConfiguration punishments;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PunishmentManager(GamingBlock plugin) {
        this.plugin = plugin;
        this.punishments = plugin.getFileManager().getFileData(FILE_NAME);
    }

    public PunishmentResult punish(Punishment punishment) {
        if (punishment == null
                || punishment.getTarget() == null
                || punishment.getType() == null) {
            return new PunishmentResult(punishment, false);
        }

        if (punishment.getType() == PunishmentType.UNBAN) {
            return new PunishmentResult(punishment, false);
        }

        if (punishment.getType() == PunishmentType.TEMPBAN
                && (punishment.getEndsOn() == null
                || !punishment.getEndsOn().toInstant().isAfter(Instant.now()))) {
            return new PunishmentResult(punishment, false);
        }

        PunishmentEntry entry = PunishmentEntry.fromPunishment(punishment);
        if (!addAndSaveHistoryEntry(entry)) {
            return new PunishmentResult(punishment, false);
        }

        if (punishment.getType() == PunishmentType.BAN
                || punishment.getType() == PunishmentType.TEMPBAN
                || punishment.getType() == PunishmentType.KICK) {
            kickTarget(punishment, entry);
        }

        return new PunishmentResult(punishment, true);
    }

    public boolean unban(
            OfflinePlayer target,
            Player operator,
            String reason
    ) {
        if (target == null) {
            return false;
        }

        String operatorName = operator == null
                ? "CONSOLE"
                : operator.getName();

        PunishmentEntry entry = PunishmentEntry.unban(
                target.getUniqueId(),
                target.getName() == null ? target.getUniqueId().toString() : target.getName(),
                operatorName,
                reason == null ? "" : reason
        );

        return addAndSaveHistoryEntry(entry);
    }

    public List<PunishmentEntry> getHistory(UUID playerUuid) {
        if (playerUuid == null) {
            return List.of();
        }

        String path = historyPath(playerUuid);
        List<?> rawEntries = punishments.getList(path, List.of());

        List<PunishmentEntry> entries = new ArrayList<>();

        for (Object rawEntry : rawEntries) {
            if (!(rawEntry instanceof java.util.Map<?, ?> map)) {
                continue;
            }

            try {
                entries.add(PunishmentEntry.fromMap(map));
            } catch (IllegalArgumentException | DateTimeParseException exception) {
                plugin.getLogger().warning(
                        "Could not load punishment history entry for " + playerUuid
                );
            }
        }

        return Collections.unmodifiableList(entries);
    }

    public Optional<PunishmentEntry> getLatestEntry(UUID playerUuid) {
        List<PunishmentEntry> history = getHistory(playerUuid);

        if (history.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(history.get(history.size() - 1));
    }

    public Optional<PunishmentEntry> getLatestRestriction(UUID playerUuid) {
        List<PunishmentEntry> history = getHistory(playerUuid);

        for (int index = history.size() - 1; index >= 0; index--) {
            PunishmentEntry entry = history.get(index);

            if (entry.getType() == PunishmentType.BAN
                    || entry.getType() == PunishmentType.TEMPBAN
                    || entry.getType() == PunishmentType.UNBAN) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    public boolean isBanned(UUID playerUuid) {
        Optional<PunishmentEntry> latestRestriction =
                getLatestRestriction(playerUuid);

        if (latestRestriction.isEmpty()) {
            return false;
        }

        PunishmentEntry entry = latestRestriction.get();

        if (entry.getType() == PunishmentType.UNBAN) {
            return false;
        }

        return entry.isActiveBan();
    }

    public Optional<PunishmentEntry> getActiveBan(UUID playerUuid) {
        Optional<PunishmentEntry> latestRestriction =
                getLatestRestriction(playerUuid);

        if (latestRestriction.isEmpty()) {
            return Optional.empty();
        }

        PunishmentEntry entry = latestRestriction.get();

        if (!entry.isActiveBan()) {
            return Optional.empty();
        }

        return Optional.of(entry);
    }

    private void addHistoryEntry(PunishmentEntry entry) {
        UUID playerUuid = entry.getTargetUuid();
        String path = historyPath(playerUuid);

        List<Map<String, Object>> history = new ArrayList<>();

        for (PunishmentEntry existingEntry : getHistory(playerUuid)) {
            history.add(existingEntry.toMap());
        }

        history.add(entry.toMap());

        punishments.set(path, history);
        punishments.set(
                "players." + playerUuid + ".name",
                entry.getTargetName()
        );
    }

    private boolean addAndSaveHistoryEntry(PunishmentEntry entry) {
        String playerPath = "players." + entry.getTargetUuid();
        Object previousHistory = punishments.get(playerPath + ".history");
        Object previousName = punishments.get(playerPath + ".name");
        addHistoryEntry(entry);
        if (plugin.getFileManager().saveFileData(FILE_NAME, punishments)) {
            return true;
        }
        punishments.set(playerPath + ".history", previousHistory);
        punishments.set(playerPath + ".name", previousName);
        return false;
    }

    private void kickTarget(Punishment punishment, PunishmentEntry entry) {
        if (!punishment.getTarget().isOnline()) {
            return;
        }

        Player target = Bukkit.getPlayer(punishment.getTarget().getUniqueId());

        if (target == null) {
            return;
        }

        if (punishment.getLayout() != null) {
            target.kick(punishment.getLayout().render(entry, plugin.getServerName()));
            return;
        }

        Component message = miniMessage.deserialize(
                "<red>You have been punished.<newline>"
                        + "<gray>Type: <white>"
                        + punishment.getType().name().toLowerCase()
                        + "<newline>"
                        + "<gray>Reason: <white>"
                        + punishment.getReason()
        );

        target.kick(message);
    }

    private String historyPath(UUID playerUuid) {
        return "players." + playerUuid + ".history";
    }

}
