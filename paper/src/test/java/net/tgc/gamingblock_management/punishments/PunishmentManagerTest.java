package net.tgc.gamingblock_management.punishments;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.tgc.gamingblock_management.GamingBlock;
import net.tgc.gamingblock_management.db.DatabaseManager;
import net.tgc.gamingblock_management.util.FileManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PunishmentManagerTest {
    private final UUID uuid = UUID.randomUUID();
    private final GamingBlock plugin = mock(GamingBlock.class);
    private final FileManager files = mock(FileManager.class);
    private final DatabaseManager database = mock(DatabaseManager.class);
    private final YamlConfiguration yaml = new YamlConfiguration();
    private PunishmentManager manager;

    @BeforeEach
    void setup() {
        when(plugin.getFileManager()).thenReturn(files);
        when(plugin.getDatabaseManager()).thenReturn(database);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(files.getFileData("punishments.yml")).thenReturn(yaml);
        when(database.isEnabled()).thenReturn(true);
        manager = new PunishmentManager(plugin);
    }

    @AfterEach
    void close() {
        manager.close();
    }

    private PunishmentEntry entry(PunishmentType type, String time) {
        return new PunishmentEntry(uuid, "player", type, "operator", "reason",
                Instant.parse(time), null, null);
    }

    @Test
    void yamlBanSurvivesDatabaseEnableAndLaterDatabaseUnbanWins() throws Exception {
        PunishmentEntry ban = entry(PunishmentType.BAN, "2025-01-01T00:00:00Z");
        PunishmentEntry unban = entry(PunishmentType.UNBAN, "2025-01-02T00:00:00Z");
        yaml.set("players." + uuid + ".history", List.of(ban.toMap()));
        when(database.getHistory(uuid)).thenReturn(List.of(), List.of(unban));
        assertTrue(manager.getActiveBan(uuid).isPresent());
        List<PunishmentEntry> merged = manager.getHistory(uuid);
        assertEquals(List.of(PunishmentType.BAN, PunishmentType.UNBAN),
                merged.stream().map(PunishmentEntry::getType).toList());
        assertTrue(manager.getActiveBanFromSnapshot(uuid, merged).isEmpty());
        PunishmentEntry newBan = entry(PunishmentType.BAN, "2025-01-03T00:00:00Z");
        yaml.set("players." + uuid + ".history", List.of(ban.toMap(), newBan.toMap()));
        assertEquals(newBan.getCreatedAt(),
                manager.getActiveBanFromSnapshot(uuid, merged).orElseThrow().getCreatedAt());
    }

    @Test
    void mirrorEntriesAreDeduplicatedWithoutLosingIdenticalRecords() {
        PunishmentEntry precise = entry(PunishmentType.BAN, "2025-01-01T00:00:00.123456789Z");
        PunishmentEntry stored = entry(PunishmentType.BAN, "2025-01-01T00:00:00.123456Z");
        assertEquals(2, PunishmentManager.mergeHistory(List.of(stored), List.of(precise, precise)).size());
    }

    @Test
    void failedDatabaseWriteRemainsVisibleAndWritesDoNotBlockCaller() throws Exception {
        CountDownLatch databaseStarted = new CountDownLatch(1);
        CountDownLatch releaseDatabase = new CountDownLatch(1);
        CountDownLatch databaseFailed = new CountDownLatch(1);
        CountDownLatch yamlSaved = new CountDownLatch(1);
        doAnswer(invocation -> {
            databaseStarted.countDown();
            if (!releaseDatabase.await(5, TimeUnit.SECONDS)) throw new SQLException("timed out");
            databaseFailed.countDown();
            throw new SQLException("write failed");
        }).when(database).save(any());
        when(files.saveFileData(eq("punishments.yml"), any())).thenAnswer(invocation -> {
            yamlSaved.countDown();
            return true;
        });
        OfflinePlayer target = mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(uuid);
        when(target.getName()).thenReturn("player");
        try {
            assertTrue(manager.unban(target, null, "reason"));
            assertTrue(databaseStarted.await(5, TimeUnit.SECONDS));
            assertTrue(yamlSaved.await(5, TimeUnit.SECONDS));
            assertEquals(PunishmentType.UNBAN, manager.getHistory(uuid).getFirst().getType());
        } finally {
            releaseDatabase.countDown();
        }
        assertTrue(databaseFailed.await(5, TimeUnit.SECONDS));
        assertEquals(1, manager.getHistory(uuid).size());
    }

    @Test
    void preLoginRejectsYamlBanAndValidationUsesDatabaseUnbanSnapshot() throws Exception {
        when(plugin.getPunishmentManager()).thenReturn(manager);
        PunishmentEntry ban = entry(PunishmentType.BAN, "2025-01-01T00:00:00Z");
        PunishmentEntry unban = entry(PunishmentType.UNBAN, "2025-01-02T00:00:00Z");
        yaml.set("players." + uuid + ".history", List.of(ban.toMap()));
        AsyncPlayerPreLoginEvent preLogin = mock(AsyncPlayerPreLoginEvent.class);
        when(preLogin.getUniqueId()).thenReturn(uuid);
        PunishmentListener listener = new PunishmentListener(plugin);
        listener.preLogin(preLogin);
        verify(preLogin).disallow(eq(AsyncPlayerPreLoginEvent.Result.KICK_BANNED),
                any(net.kyori.adventure.text.Component.class));

        when(database.getHistory(uuid)).thenReturn(List.of(unban));
        listener.preLogin(preLogin);
        PlayerConnectionValidateLoginEvent validate = mock(PlayerConnectionValidateLoginEvent.class);
        PlayerLoginConnection connection = mock(PlayerLoginConnection.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        when(validate.getConnection()).thenReturn(connection);
        when(connection.getAuthenticatedProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(uuid);
        listener.validateLogin(validate);
        verify(validate, never()).kickMessage(any());
    }

    @Test
    void fallsBackToYamlWhenDatabaseReadFails() throws Exception {
        PunishmentEntry ban = entry(PunishmentType.BAN, "2025-01-01T00:00:00Z");
        yaml.set("players." + uuid + ".history", List.of(ban.toMap()));
        when(database.getHistory(uuid)).thenThrow(new SQLException("offline"));
        assertTrue(manager.getActiveBan(uuid).isPresent());
    }
}
