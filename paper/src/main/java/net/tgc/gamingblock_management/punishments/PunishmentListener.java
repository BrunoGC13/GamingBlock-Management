package net.tgc.gamingblock_management.punishments;

import net.kyori.adventure.text.Component;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class PunishmentListener implements Listener {
    private final GamingBlock plugin;
    // Also bounds entries for connections that never reach a quit event.
    private final Map<UUID, List<PunishmentEntry>> loginHistory =
            Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, List<PunishmentEntry>> eldest) {
                    return size() > 10000;
                }
            });

    public PunishmentListener(GamingBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void preLogin(AsyncPlayerPreLoginEvent event) {
        List<PunishmentEntry> history =
                plugin.getPunishmentManager().getHistory(event.getUniqueId());
        Optional<PunishmentEntry> ban = PunishmentManager.activeBan(history);
        if (ban.isPresent()) {
            loginHistory.remove(event.getUniqueId());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    getPunishmentMessage(ban.get()));
        } else {
            loginHistory.put(event.getUniqueId(), history);
        }
    }

    @EventHandler
    public void validateLogin(PlayerConnectionValidateLoginEvent event) {
        PlayerProfile profile;
        if (event.getConnection() instanceof PlayerLoginConnection connection) {
            profile = connection.getAuthenticatedProfile();
        } else if (event.getConnection() instanceof PlayerConfigurationConnection connection) {
            profile = connection.getProfile();
        } else {
            return;
        }

        if (profile == null || profile.getId() == null) return;

        List<PunishmentEntry> history = loginHistory.get(profile.getId());
        if (history == null) return;
        Optional<PunishmentEntry> activeBan = plugin.getPunishmentManager()
                .getActiveBanFromSnapshot(profile.getId(), history);

        if (activeBan.isEmpty()) {
            return;
        }

        PunishmentEntry entry = activeBan.get();

        Component message = getPunishmentMessage(entry);

        event.kickMessage(message);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        loginHistory.remove(event.getPlayer().getUniqueId());
    }

    private Component getPunishmentMessage(PunishmentEntry entry) {
        if (entry.getLayout() != null) {
            Optional<PunishmentLayout> layout =
                    plugin.getPunishmentLayoutRegistry()
                            .get(entry.getLayout());

            if (layout.isPresent()) {
                return layout.get().render(entry, plugin.getServerName());
            }

            plugin.getLogger().warning(
                    "Could not find punishment layout '"
                            + entry.getLayout()
                            + "' for player "
                            + entry.getTargetName()
            );
        }

        return Component.text(
                "You are banned from this server.\n"
                        + "Reason: " + entry.getReason()
                        + "\nOperator: " + entry.getOperatorName()
        );
    }
}
