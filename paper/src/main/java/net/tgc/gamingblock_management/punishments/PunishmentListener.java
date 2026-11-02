package net.tgc.gamingblock_management.punishments;

import net.kyori.adventure.text.Component;
import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;

public class PunishmentListener implements Listener {
    private final GamingBlock plugin;

    public PunishmentListener(GamingBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void beforePlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Optional<PunishmentEntry> activeBan =
                plugin.getPunishmentManager()
                        .getActiveBan(player.getUniqueId());

        if (activeBan.isEmpty()) {
            return;
        }

        PunishmentEntry entry = activeBan.get();

        Component message = getPunishmentMessage(entry);

        player.kick(message);
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