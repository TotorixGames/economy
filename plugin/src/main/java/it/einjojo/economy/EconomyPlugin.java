package it.einjojo.economy;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import it.einjojo.economy.db.PostgresEconomyRepository;
import it.einjojo.economy.hook.TinyMarketsHook;
import it.einjojo.economy.hook.ZShopHook;
import it.einjojo.economy.listener.ConnectionListener;
import it.einjojo.economy.util.SharedConnectionConfiguration;
import it.einjojo.economy.vault.ReasonProvider;
import it.einjojo.economy.vault.VaultEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

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
        SharedConnectionConfiguration config = SharedConnectionConfiguration.load();
        var hikariConfig = config.getPostgres().createHikariConfig();
        hikariConfig.setMaximumPoolSize(3);
        hikariConfig.setMinimumIdle(3);
        hikariConfig.setPoolName("EconomyProvider");
        try {
            dataSource = new HikariDataSource(hikariConfig);
        } catch (HikariPool.PoolInitializationException poolInitializationException) {
            getSLF4JLogger().error("Failed to connect to the database using {}, disabling plugin!", config.getPostgres(), poolInitializationException);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        PostgresEconomyRepository repository = new PostgresEconomyRepository(dataSource::getConnection, "economy");
        economyService = new DefaultEconomyService(repository, null, executorService);
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
