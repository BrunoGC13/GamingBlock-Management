package net.tgc.gamingblock_management.db;

import net.tgc.gamingblock_management.GamingBlock;
import net.tgc.gamingblock_management.punishments.PunishmentEntry;
import net.tgc.gamingblock_management.punishments.PunishmentType;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class DatabaseManager implements AutoCloseable {
    private static final String TABLE_NAME = "gbm_punishments";

    private final GamingBlock plugin;
    private final String serverId;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private boolean enabled;

    public DatabaseManager(GamingBlock plugin) {
        this.plugin = plugin;

        FileConfiguration config = plugin.getConfig();

        this.serverId = config.getString("server-id", "default");
        this.username = config.getString("database.user", "");
        this.password = config.getString("database.password", "");

        boolean configuredEnabled =
                config.getBoolean("database.enabled", false);

        this.jdbcUrl = createJdbcUrl(config);

        if (!configuredEnabled) {
            plugin.getLogger().info(
                    "External database is disabled in config.yml."
            );
            this.enabled = false;
            return;
        }

        if (jdbcUrl.isBlank()
                || username.isBlank()
                || password.isBlank()) {
            plugin.getLogger().warning(
                    "External database is enabled, "
                            + "but its configuration is incomplete. "
                            + "Falling back to YAML storage."
            );
            this.enabled = false;
            return;
        }

        try {
            initializeSchema();

            this.enabled = true;

            plugin.getLogger().info(
                    "External database connected successfully."
            );
        } catch (SQLException exception) {
            this.enabled = false;

            plugin.getLogger().severe(
                    "Could not initialize the external database. "
                            + "Falling back to YAML storage. Error: "
                            + exception.getMessage()
            );
        }
    }

    private String createJdbcUrl(FileConfiguration config) {
        String configuredUrl =
                config.getString("database.jdbc_url", "");

        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl.trim();
        }

        String ip = config.getString("database.ip", "");
        int port = config.getInt("database.port", 3306);
        String database = config.getString(
                "database.database",
                "gamingblock_management"
        );

        if (ip == null || ip.isBlank() || database.isBlank()) {
            return "";
        }

        return "jdbc:mariadb://"
                + ip
                + ":"
                + port
                + "/"
                + database
                + "?sslMode=verify-full&serverTimezone=UTC&socketTimeout=10000";
    }

    public boolean isEnabled() {
        return enabled;
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException(
                    "MariaDB JDBC driver is not included in the plugin JAR",
                    exception
            );
        }

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("socketTimeout", "10000");
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    private void initializeSchema() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS gbm_punishments (
                    id BIGINT NOT NULL AUTO_INCREMENT,
                    server_id VARCHAR(128) NOT NULL,
                    target_uuid CHAR(36) NOT NULL,
                    target_name VARCHAR( Minecraft 255 ) NOT NULL,
                    punishment_type VARCHAR(32) NOT NULL,
                    operator_name VARCHAR(255) NOT NULL,
                    reason TEXT NOT NULL,
                    created_at TIMESTAMP(6) NOT NULL,
                    ends_at TIMESTAMP(6) NULL,
                    layout_name VARCHAR(255) NULL,
                    PRIMARY KEY (id),
                    INDEX idx_gbm_target_uuid (target_uuid),
                    INDEX idx_gbm_target_created (target_uuid, created_at, id)
                )
                """.replace("VARCHAR( Minecraft 255 )", "VARCHAR(255)");

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public void save(PunishmentEntry entry) throws SQLException {
        String sql = """
                INSERT INTO gbm_punishments (
                    server_id,
                    target_uuid,
                    target_name,
                    punishment_type,
                    operator_name,
                    reason,
                    created_at,
                    ends_at,
                    layout_name
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            statement.setString(2, entry.getTargetUuid().toString());
            statement.setString(3, entry.getTargetName());
            statement.setString(4, entry.getType().name());
            statement.setString(5, entry.getOperatorName());
            statement.setString(6, entry.getReason());
            statement.setTimestamp(
                    7,
                    Timestamp.from(entry.getCreatedAt())
            );

            if (entry.getEndsAt() == null) {
                statement.setTimestamp(8, null);
            } else {
                statement.setTimestamp(
                        8,
                        Timestamp.from(entry.getEndsAt())
                );
            }

            statement.setString(9, entry.getLayout());
            statement.executeUpdate();
        }
    }

    public List<PunishmentEntry> getHistory(UUID targetUuid)
            throws SQLException {
        String sql = """
                SELECT
                    target_uuid,
                    target_name,
                    punishment_type,
                    operator_name,
                    reason,
                    created_at,
                    ends_at,
                    layout_name
                FROM gbm_punishments
                WHERE target_uuid = ?
                ORDER BY created_at ASC, id ASC
                """;

        List<PunishmentEntry> entries = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetUuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(fromResultSet(resultSet));
                }
            }
        }

        return List.copyOf(entries);
    }

    private PunishmentEntry fromResultSet(ResultSet resultSet)
            throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp endsAt = resultSet.getTimestamp("ends_at");

        return new PunishmentEntry(
                UUID.fromString(resultSet.getString("target_uuid")),
                resultSet.getString("target_name"),
                PunishmentType.valueOf(
                        resultSet.getString("punishment_type")
                                .toUpperCase()
                ),
                resultSet.getString("operator_name"),
                resultSet.getString("reason"),
                createdAt.toInstant(),
                endsAt == null ? null : endsAt.toInstant(),
                resultSet.getString("layout_name")
        );
    }

    @Override
    public void close() {
        // Connections are intentionally short-lived and closed per operation.
    }
}