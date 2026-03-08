package it.einjojo.economy.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @version 2.0.0
 */
@Getter
public class SharedConnectionConfiguration {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private RedisConnectionConfiguration redis = new RedisConnectionConfiguration("localhost", 6379, "default", "default", false);
    private PostgresConfiguration postgres = new PostgresConfiguration(
            "jdbc:postgresql://localhost:5432/postgres",
            "postgres",
            "postgres"
    );


    public static SharedConnectionConfiguration load() {
        Path configFile = Paths.get("connections.json");
        try {
            if (!Files.exists(configFile)) {
                SharedConnectionConfiguration defaultConfig = new SharedConnectionConfiguration();
                String json = GSON.toJson(defaultConfig);
                Files.write(configFile, json.getBytes());
                return defaultConfig;
            }
            String json = new String(Files.readAllBytes(configFile));
            return GSON.fromJson(json, SharedConnectionConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load connections.json", e);
        }
    }

    public record RedisConnectionConfiguration(String host, int port, String username, String password,
                                               boolean ssl) {
        public RedisURI createUri(String clientName) {
            return RedisURI.builder()
                    .withHost(host)
                    .withPort(port)
                    .withAuthentication(username, password)
                    .withClientName(clientName)
                    .withSsl(ssl)
                    .build();
        }
    }

    public record PostgresConfiguration(String jdbcUrl, String username, String password) {


        public HikariConfig createHikariConfig() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("org.postgresql.Driver");
            config.setSchema("public");
            return config;
        }

        @Override
        public @NotNull String toString() {
            return "PostgresConfiguration{" +
                    "jdbcUrl='" + jdbcUrl + '\'' +
                    ", username='" + username + '\'' +
                    ", password='" + "*".repeat(password.length()) + '\'' +
                    '}';
        }
    }

}