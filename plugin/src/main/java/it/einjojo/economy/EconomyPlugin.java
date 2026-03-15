package it.einjojo.economy;

import com.zaxxer.hikari.HikariDataSource;
import it.einjojo.economy.db.EconomyRepository;
import it.einjojo.economy.db.PostgresEconomyRepository;
import it.einjojo.economy.db.SqliteEconomyRepository;
import it.einjojo.economy.hook.TinyMarketsHook;
import it.einjojo.economy.hook.ZShopHook;
import it.einjojo.economy.listener.ConnectionListener;
import it.einjojo.economy.util.SQLLiteConnection;
import it.einjojo.economy.util.SharedConnectionConfiguration;
import it.einjojo.economy.vault.ReasonProvider;
import it.einjojo.economy.vault.VaultEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EconomyPlugin extends JavaPlugin {
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final OnlinePlayerEconomyCache cache = new OnlinePlayerEconomyCache();
    private HikariDataSource dataSource;
    private DefaultEconomyService economyService;

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            registerService(Economy.class, new VaultEconomy(ReasonProvider.DEFAULT_VAULT, this));
            getSLF4JLogger().info("Registered Vault Economy implementation!");
        }
    }

    @Override
    public void onEnable() {

        EconomyRepository economyRepository;
        try {
            SharedConnectionConfiguration config = SharedConnectionConfiguration.load();
            var hikariConfig = config.getPostgres().createHikariConfig();
            hikariConfig.setMaximumPoolSize(3);
            hikariConfig.setMinimumIdle(3);
            hikariConfig.setPoolName("EconomyProvider");
            dataSource = new HikariDataSource(hikariConfig);
            economyRepository = new PostgresEconomyRepository(dataSource::getConnection, "economy");
        } catch (Exception exception) {
            getSLF4JLogger().warn("""
                    Failed to initialize Postgres connection, falling back to SQLite. 
                    This may be caused by a missing or invalid config file, or by a failure to connect 
                    to the database. Check the logs for more details.
                    """, exception);
            try {
                economyRepository = new SqliteEconomyRepository(new SQLLiteConnection(this));
            } catch (SQLException e) {
                getSLF4JLogger().error("Failed to initialize economy repository, plugin cannot function!", e);
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        economyService = new DefaultEconomyService(economyRepository, null, executorService);
        economyService.initialize();
        economyService.setSyncCache(cache);
        registerService(EconomyService.class, economyService);
        registerService(EconomyCache.class, cache);
        getServer().getPluginManager().registerEvents(new ConnectionListener(economyService, cache), this);

        try {
            if (Bukkit.getPluginManager().getPlugin("TinyMarkets") != null) {
                new TinyMarketsHook(this).init();
            }
            if (Bukkit.getPluginManager().getPlugin("zShop") != null) {
                new ZShopHook(this).init();
            }
        } catch (Exception exception) {
            getSLF4JLogger().error("Failed to hook into one of the supported plugins, some features may not work correctly!", exception);
        }
    }

    @Override
    public void onDisable() {
        if (dataSource != null) {
            dataSource.close();
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            getSLF4JLogger().error("Failed to shutdown economy executor service cleanly, forcing shutdown now!", e);
            executorService.shutdownNow();
        }

    }


    public DefaultEconomyService economyService() {
        return economyService;
    }

    public HikariDataSource dataSource() {
        return dataSource;
    }

    public EconomyCache cache() {
        return cache;
    }

    public ExecutorService executorService() {
        return executorService;
    }

    public <T> void registerService(Class<T> clazz, T instance) {
        Bukkit.getServicesManager().register(clazz, instance, this, ServicePriority.Normal);
    }

}
