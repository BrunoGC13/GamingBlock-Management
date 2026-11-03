package net.tgc.gamingblock_management.db;

import net.tgc.gamingblock_management.GamingBlock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.Configuration;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseManagerTest {
    private String jdbcUrl(YamlConfiguration config) throws Exception {
        GamingBlock plugin = mock(GamingBlock.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        DatabaseManager manager = new DatabaseManager(plugin);
        Field field = DatabaseManager.class.getDeclaredField("jdbcUrl");
        field.setAccessible(true);
        return (String) field.get(manager);
    }

    @Test
    void generatedUrlUsesVerifiedTlsAndBoundedSocketTimeout() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.ip", "db.example.org");
        String url = jdbcUrl(config);
        assertTrue(url.contains("sslMode=verify-full"));
        assertTrue(url.contains("serverTimezone=UTC"));
        assertTrue(url.contains("socketTimeout=10000"));
        assertEquals(10000, Configuration.parse(url).socketTimeout());
    }

    @Test
    void explicitNonTlsUrlIsPreserved() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        String url = "jdbc:mariadb://localhost:3306/test?sslMode=disable&serverTimezone=UTC";
        config.set("database.jdbc_url", "  " + url + "  ");
        assertEquals(url, jdbcUrl(config));
    }
}
