package it.einjojo.economy.util;

import it.einjojo.economy.db.ConnectionProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLLiteConnection implements ConnectionProvider {
    private final Connection connection;

    public SQLLiteConnection(JavaPlugin plugin) throws SQLException {
        Path file = plugin.getDataPath().resolve("economy.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }
}
