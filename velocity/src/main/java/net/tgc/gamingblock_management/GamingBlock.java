package net.tgc.gamingblock_management;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import net.tgc.gamingblock_management.commands.SuspicionCommand;
import net.tgc.gamingblock_management.suspicions.SuspicionsManager;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "gamingblock_management",
        name = "GamingBlock Management",
        version = BuildConstants.VERSION,
        authors = "BrunoGC13",
        url = "https://brunoglotz.com ; https://gamingblock.de"
)
public final class GamingBlock {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final SuspicionsManager suspicionsManager;

    @Inject
    public GamingBlock(
            ProxyServer server,
            Logger logger,
            @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory
    ) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.suspicionsManager = new SuspicionsManager(
                dataDirectory,
                logger
        );

        logger.info("GamingBlock Management initialized");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getCommandManager().register(
                "suspection",
                SuspicionCommand.createBrigadierCommand(
                        server,
                        suspicionsManager
                )
        );

        logger.info("Suspection command registered");
    }

    public SuspicionsManager getSuspectionsManager() {
        return suspicionsManager;
    }
}