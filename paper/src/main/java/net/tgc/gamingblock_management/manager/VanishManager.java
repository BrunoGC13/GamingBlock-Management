package net.tgc.gamingblock_management.manager;

import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;

public class VanishManager implements Listener {
    private GamingBlock plugin;
    Set<Player> vanishedPlayers;

    public VanishManager(GamingBlock plugin) {
        this.plugin = plugin;
        this.vanishedPlayers = new HashSet<>();
    }

    public boolean setVanish(Player p) {
        if (vanishedPlayers.contains(p)) {
            vanishedPlayers.remove(p);

            for (Player other : Bukkit.getOnlinePlayers()) {
                other.showPlayer(plugin, p);
            }

            return false;
        }

        vanishedPlayers.add(p);
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.hidePlayer(plugin, p);
        }
        return true;
    }

    public boolean getVanish(Player p) {
        return vanishedPlayers.contains(p);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!vanishedPlayers.contains(e.getPlayer())) return;

        Player p = e.getPlayer();

        boolean vanish = setVanish(p);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        for (Player normal : Bukkit.getOnlinePlayers()) {
            if (vanishedPlayers.contains(normal)) {
                e.getPlayer().hidePlayer(plugin, normal);
                continue;
            }
            e.getPlayer().showPlayer(plugin, normal);
        }
    }

}
