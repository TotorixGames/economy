package it.einjojo.economy;

import it.einjojo.economy.vault.ReasonProvider;
import it.einjojo.economy.vault.VaultEconomy;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            registerService(Economy.class, new VaultEconomy(ReasonProvider.DEFAULT_VAULT, this));
            getSLF4JLogger().info("Registered Vault Economy implementation!");
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }


    public <T> void registerService(Class<T> clazz, T instance) {
        Bukkit.getServicesManager().register(clazz, instance, this, ServicePriority.Normal);
    }

    public EconomyCache cache() {
        return null;
    }

    public EconomyService economyService() {


    }
}
