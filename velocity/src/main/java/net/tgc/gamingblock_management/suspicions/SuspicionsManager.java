package net.tgc.gamingblock_management.suspicions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SuspicionsManager {
    private static final Type STORAGE_TYPE =
            new TypeToken<List<Suspicion>>() {
            }.getType();

    private final Logger logger;
    private final Path file;
    private final Gson gson;
    private final Map<UUID, Suspicion> suspicions = new HashMap<>();

    public SuspicionsManager(Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.file = dataDirectory.resolve("suspicions.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        loadSuspicions();
    }

    public synchronized Suspicion createSuspicion(
            UUID targetUuid,
            String targetName,
            UUID issuerUuid,
            String issuerName,
            SuspicionType type,
            String reason
    ) {
        if (targetUuid == null
                || targetName == null
                || issuerUuid == null
                || issuerName == null
                || type == null
                || reason == null
                || reason.isBlank()) {
            return null;
        }

        if (getSuspicion(targetUuid) != null) {
            return null;
        }

        Suspicion suspicion = new Suspicion(
                targetUuid,
                targetName,
                issuerUuid,
                issuerName,
                type,
                reason.trim()
        );

        suspicions.put(targetUuid, suspicion);
        saveSuspicions();

        return suspicion;
    }

    public synchronized Suspicion getSuspicion(UUID targetUuid) {
        if (targetUuid == null) {
            return null;
        }

        return suspicions.get(targetUuid);
    }

    public synchronized Suspicion getSuspicion(String targetUuid) {
        if (targetUuid == null || targetUuid.isBlank()) {
            return null;
        }

        try {
            return getSuspicion(UUID.fromString(targetUuid));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public synchronized boolean editSuspicion(
            UUID targetUuid,
            SuspicionType type,
            String reason
    ) {
        Suspicion suspicion = getSuspicion(targetUuid);

        if (suspicion == null
                || type == null
                || reason == null
                || reason.isBlank()) {
            return false;
        }

        suspicion
                .setType(type)
                .setReason(reason.trim())
                .setFinished(false);

        saveSuspicions();
        return true;
    }

    public synchronized boolean removeSuspicion(UUID targetUuid) {
        if (targetUuid == null) {
            return false;
        }

        Suspicion removed = suspicions.remove(targetUuid);

        if (removed == null) {
            return false;
        }

        saveSuspicions();
        return true;
    }

    public synchronized boolean finishSuspicion(UUID targetUuid) {
        Suspicion suspicion = getSuspicion(targetUuid);

        if (suspicion == null) {
            return false;
        }

        suspicion.setFinished(true);
        saveSuspicions();

        return true;
    }

    public synchronized List<Suspicion> getSuspicions() {
        return List.copyOf(suspicions.values());
    }

    private void loadSuspicions() {
        suspicions.clear();

        if (!Files.exists(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            List<Suspicion> loaded = gson.fromJson(reader, STORAGE_TYPE);

            if (loaded == null) {
                return;
            }

            for (Suspicion suspicion : loaded) {
                if (suspicion == null
                        || suspicion.getTargetUuid() == null
                        || suspicion.getType() == null
                        || suspicion.getReason() == null) {
                    logger.warn("Skipping invalid suspicion entry.");
                    continue;
                }

                if (suspicion.isFinished()) {
                    continue;
                }

                suspicions.put(suspicion.getTargetUuid(), suspicion);
            }
        } catch (IOException | RuntimeException exception) {
            logger.error("Could not load suspicions from {}.", file, exception);
        }
    }

    private void saveSuspicions() {
        try {
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(file)) {
                gson.toJson(new ArrayList<>(suspicions.values()), STORAGE_TYPE, writer);
            }
        } catch (IOException exception) {
            logger.error("Could not save suspicions to {}.", file, exception);
        }
    }
}