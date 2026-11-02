package net.tgc.gamingblock_management.punishments;

import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PunishmentLayoutRegistry {
    private final GamingBlock plugin;
    private final Map<String, PunishmentLayout> layouts = new HashMap<>();
    private final String layoutFileName = "layouts.yml";
    private FileConfiguration layoutData;

    public PunishmentLayoutRegistry(GamingBlock plugin) {
        this.plugin = plugin;
        this.layoutData = plugin.getFileManager().getFileData(layoutFileName);
    }

    public void loadAll() {
        ConfigurationSection layouts = layoutData.getConfigurationSection("layouts");

        if (layouts == null) return;

        for (String key : layouts.getKeys(false)) {
            String[] messages = layouts.getStringList(key + ".messages").toArray(new String[0]);
            String name = layouts.getString(key + ".name");

            register(new PunishmentLayout(name, messages));
        }
    }

    public void register(PunishmentLayout layout) {
        if (layout == null || layout.getName() == null) {
            return;
        }

        layouts.put(layout.getName().toLowerCase(), layout);
    }

    public Optional<PunishmentLayout> get(String name) {
        if (name == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                layouts.get(name.toLowerCase())
        );
    }

    public boolean contains(String name) {
        return get(name).isPresent();
    }
}
