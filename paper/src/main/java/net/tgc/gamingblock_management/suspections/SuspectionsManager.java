package net.tgc.gamingblock_management.suspections;

import net.tgc.gamingblock_management.Gamingblock;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SuspectionsManager {
    private final Gamingblock plugin;
    private final String fileName = "suspections.yml";
    private final Map<String, Suspection> suspections = new HashMap<>();

    private FileConfiguration data;

    public SuspectionsManager(Gamingblock plugin) {
        this.plugin = plugin;
        reloadData();
    }

    private void reloadData() {
        this.data = plugin.getFileManager().getFileData(fileName);

        if (this.data == null) {
            plugin.getLogger().warning("Could not load " + fileName + ".");
        }
    }

    public Suspection createSuspection(
            Player target,
            Player issuer,
            SuspectionType type,
            String reason
    ) {
        if (target == null || issuer == null || type == null || reason == null || reason.isBlank()) {
            return null;
        }

        if (getSuspection(target) != null) {
            return null;
        }

        Suspection suspection = new Suspection(target, issuer, type, reason.trim());
        suspections.put(target.getName().toLowerCase(Locale.ROOT), suspection);
        saveSuspection(target.getName(), suspection);

        return suspection;
    }

    public Suspection getSuspection(Player target) {
        if (target == null) {
            return null;
        }

        return getSuspection(target.getName());
    }

    public Suspection getSuspection(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }

        return suspections.get(targetName.toLowerCase(Locale.ROOT));
    }

    public boolean editSuspection(
            Player target,
            SuspectionType type,
            String reason
    ) {
        Suspection suspection = getSuspection(target);

        if (suspection == null || type == null || reason == null || reason.isBlank()) {
            return false;
        }

        suspection
                .setType(type)
                .setReason(reason.trim())
                .setFinished(false);

        saveSuspection(target.getName(), suspection);
        return true;
    }

    public boolean removeSuspection(Player target) {
        if (target == null) {
            return false;
        }

        return removeSuspection(target.getName());
    }

    public boolean removeSuspection(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return false;
        }

        String key = targetName.toLowerCase(Locale.ROOT);
        Suspection removed = suspections.remove(key);

        if (removed == null) {
            return false;
        }

        data.set("suspections." + targetName, null);
        plugin.getFileManager().saveFileData(fileName);
        return true;
    }

    public boolean finishSuspection(Player target) {
        Suspection suspection = getSuspection(target);

        if (suspection == null) {
            return false;
        }

        suspection.setFinished(true);
        saveSuspection(target.getName(), suspection);
        return true;
    }

    private void saveSuspection(String targetName, Suspection suspection) {
        if (data == null) {
            return;
        }

        String path = "suspections." + targetName;

        data.set(path + ".name", targetName);
        data.set(path + ".issuer", suspection.getIssuer().getName());
        data.set(path + ".type", suspection.getType().name());
        data.set(path + ".reason", suspection.getReason());
        data.set(path + ".finished", suspection.isFinished());

        plugin.getFileManager().saveFileData(fileName);
    }

    public void loadSuspections() {
        reloadData();
        suspections.clear();

        if (data == null) {
            return;
        }

        ConfigurationSection section = data.getConfigurationSection("suspections");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String targetName = section.getString(key + ".name");
            String issuerName = section.getString(key + ".issuer");
            String typeName = section.getString(key + ".type");
            String reason = section.getString(key + ".reason");
            boolean finished = section.getBoolean(key + ".finished", false);

            if (targetName == null
                    || issuerName == null
                    || typeName == null
                    || reason == null) {
                plugin.getLogger().warning("Skipping invalid suspicion entry: " + key);
                continue;
            }

            if (finished) {
                continue;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            Player issuer = Bukkit.getPlayerExact(issuerName);

            if (target == null || issuer == null) {
                plugin.getLogger().warning(
                        "Could not load suspicion for " + targetName
                                + " because the target or issuer is offline."
                );
                continue;
            }

            SuspectionType type;

            try {
                type = SuspectionType.valueOf(typeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Invalid suspicion type '" + typeName
                                + "' for " + targetName
                );
                continue;
            }

            Suspection suspection = new Suspection(target, issuer, type, reason);
            suspections.put(targetName.toLowerCase(Locale.ROOT), suspection);
        }
    }
}