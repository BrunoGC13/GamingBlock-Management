package net.tgc.gamingblock_management.punishments;

import net.kyori.adventure.text.Component;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class PunishmentListener implements Listener {
    private final GamingBlock plugin;

    public PunishmentListener(GamingBlock plugin) {
        this.plugin = plugin;
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

        Optional<PunishmentEntry> activeBan =
                plugin.getPunishmentManager()
                        .getActiveBan(profile.getId());

        if (activeBan.isEmpty()) {
            return;
        }

        PunishmentEntry entry = activeBan.get();

        Component message = getPunishmentMessage(entry);

        event.kickMessage(message);
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
