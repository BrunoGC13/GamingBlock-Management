package net.tgc.gamingblock_management.punishments;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.tgc.gamingblock_management.GamingBlock;
import net.tgc.gamingblock_management.util.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PunishmentBehaviorTest {
    private final UUID uuid = UUID.randomUUID();
    private final GamingBlock plugin = mock(GamingBlock.class);
    private final FileManager files = mock(FileManager.class);
    private final YamlConfiguration data = new YamlConfiguration();
    private final OfflinePlayer target = mock(OfflinePlayer.class);
    private PunishmentManager manager;

    @BeforeEach
    void setUp() {
        when(plugin.getFileManager()).thenReturn(files);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(files.getFileData("punishments.yml")).thenReturn(data);
        when(files.saveFileData(eq("punishments.yml"), same(data))).thenReturn(true);
        when(target.getUniqueId()).thenReturn(uuid);
        when(target.getName()).thenReturn("Target");
        manager = new PunishmentManager(plugin);
        when(plugin.getPunishmentManager()).thenReturn(manager);
        when(plugin.getServerName()).thenReturn("Server");
    }

    private String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void loadsAndRendersLayoutsForCurrentEntry() {
        YamlConfiguration layouts = new YamlConfiguration();
        layouts.set("layouts.default.name", "default");
        layouts.set("layouts.default.messages", List.of("%SERVER_NAME%", "", "%REASON%", "%ENDS_ON%"));
        when(files.getFileData("layouts.yml")).thenReturn(layouts);
        PunishmentLayoutRegistry registry = new PunishmentLayoutRegistry(plugin);
        registry.loadAll();
        PunishmentLayout layout = registry.get("DEFAULT").orElseThrow();
        PunishmentEntry entry = new PunishmentEntry(uuid, "Target", PunishmentType.TEMPBAN,
                "Admin", "Reason", Instant.EPOCH, Instant.parse("2030-01-02T03:04:05Z"), "default");
        assertEquals("Server\n\nReason\n2030-01-02T03:04:05Z", plain(layout.render(entry, "Server")));
        PunishmentEntry permanent = new PunishmentEntry(uuid, "Target", PunishmentType.BAN,
                "Admin", "Other", Instant.EPOCH, null, "default");
        assertEquals("Server\n\nOther\nNever", plain(layout.render(permanent, "Server")));
    }

    @Test
    void rejectsInvalidTemporaryBansWithoutSavingOrKicking() {
        for (Date end : new Date[]{null, Date.from(Instant.now().minusSeconds(10))}) {
            assertFalse(manager.punish(new Punishment(target, null, PunishmentType.TEMPBAN, "reason", end)).isSuccessful());
        }
        assertTrue(manager.getHistory(uuid).isEmpty());
        verify(files, never()).saveFileData(anyString(), any());
        verify(target, never()).isOnline();
        assertTrue(manager.punish(new Punishment(target, null, PunishmentType.TEMPBAN,
                "reason", Date.from(Instant.now().plusSeconds(3600)))).isSuccessful());
        assertTrue(manager.isBanned(uuid));
    }

    @Test
    void skipsMalformedDatesAndKeepsLookingUpBans() {
        PunishmentEntry ban = new PunishmentEntry(uuid, "Target", PunishmentType.BAN,
                "Admin", "reason", Instant.EPOCH, null, null);
        Map<String, Object> malformed = new java.util.LinkedHashMap<>(ban.toMap());
        malformed.put("createdAt", "invalid");
        Map<String, Object> malformedEnd = new java.util.LinkedHashMap<>(ban.toMap());
        malformedEnd.put("endsAt", "invalid");
        data.set("players." + uuid + ".history", List.of(ban.toMap(), malformed, malformedEnd));
        assertEquals(1, manager.getHistory(uuid).size());
        assertTrue(manager.isBanned(uuid));
    }

    @Test
    void failedSavesDoNotChangeActiveBanOrKick() {
        when(files.saveFileData(eq("punishments.yml"), same(data))).thenReturn(false);
        assertFalse(manager.punish(new Punishment(target, null, PunishmentType.BAN)).isSuccessful());
        assertTrue(manager.getHistory(uuid).isEmpty());
        verify(target, never()).isOnline();
        when(files.saveFileData(eq("punishments.yml"), same(data))).thenReturn(true);
        assertTrue(manager.punish(new Punishment(target, null, PunishmentType.BAN)).isSuccessful());
        when(files.saveFileData(eq("punishments.yml"), same(data))).thenReturn(false);
        assertFalse(manager.unban(target, null, "reason"));
        assertTrue(manager.isBanned(uuid));
        assertEquals(1, manager.getHistory(uuid).size());
    }

    @Test
    void fileManagerReportsWriteFailures(@TempDir Path directory) throws IOException {
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getFileManagerData()).thenReturn(new YamlConfiguration());
        FileManager fileManager = new FileManager(plugin);
        fileManager.createFiles(new String[]{"punishments.yml"});
        FileConfiguration failing = mock(FileConfiguration.class);
        doThrow(new IOException("write failed")).when(failing).save(any(java.io.File.class));
        assertFalse(fileManager.saveFileData("punishments.yml", failing));
        assertFalse(fileManager.saveFileData("missing.yml", data));
        assertTrue(fileManager.saveFileData("punishments.yml", data));
    }

    @Test
    void kickUsesTheCurrentPunishmentWithRegistryStyleLayout() {
        Player online = mock(Player.class);
        when(target.isOnline()).thenReturn(true);
        PunishmentLayout layout = new PunishmentLayout("default", new String[]{"%REASON%"});
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(online);
            assertTrue(manager.punish(new Punishment(target, null, PunishmentType.KICK, "current", layout)).isSuccessful());
            verify(online).kick(layout.render(manager.getLatestEntry(uuid).orElseThrow(), "Server"));
        }
    }

    @Test
    void rejectsBansDuringBothLoginValidationPhases() {
        assertTrue(manager.punish(new Punishment(target, null, PunishmentType.BAN, "reason")).isSuccessful());
        PunishmentListener listener = new PunishmentListener(plugin);
        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getId()).thenReturn(uuid);
        PlayerLoginConnection login = mock(PlayerLoginConnection.class);
        when(login.getAuthenticatedProfile()).thenReturn(profile);
        PlayerConfigurationConnection configuration = mock(PlayerConfigurationConnection.class);
        when(configuration.getProfile()).thenReturn(profile);
        PlayerConnectionValidateLoginEvent loginEvent = new PlayerConnectionValidateLoginEvent(login, null);
        PlayerConnectionValidateLoginEvent configurationEvent = new PlayerConnectionValidateLoginEvent(configuration, null);
        listener.validateLogin(loginEvent);
        listener.validateLogin(configurationEvent);
        assertFalse(loginEvent.isAllowed());
        assertFalse(configurationEvent.isAllowed());
        assertTrue(plain(loginEvent.getKickMessage()).contains("reason"));
        when(profile.getId()).thenReturn(UUID.randomUUID());
        Component priorDenial = Component.text("Denied");
        PlayerConnectionValidateLoginEvent unbanned = new PlayerConnectionValidateLoginEvent(login, priorDenial);
        listener.validateLogin(unbanned);
        assertEquals(priorDenial, unbanned.getKickMessage());
    }
}
