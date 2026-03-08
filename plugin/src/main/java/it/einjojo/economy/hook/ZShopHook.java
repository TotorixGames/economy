package it.einjojo.economy.hook;


import fr.maxlego08.zshop.api.economy.ShopEconomy;
import fr.maxlego08.zshop.api.event.events.ZShopEconomyRegisterEvent;
import it.einjojo.economy.EconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ZShopHook implements Listener {
    private static final Logger log = LoggerFactory.getLogger(ZShopHook.class);
    private final EconomyPlugin plugin;

    public ZShopHook(EconomyPlugin plugin) {
        this.plugin = plugin;
    }


    public void init() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void handleEconomyRegister(ZShopEconomyRegisterEvent event) {
        var manager = event.getManager();
        if (manager != null) {
            manager.getEconomy("VAULT").ifPresent(economy -> {
                log.info("Removing VAULT economy from zShop");
                manager.removeEconomy(economy);
            });
            manager.registerEconomy(new EcoWrapper(plugin, "PGECO"));
            manager.registerEconomy(new EcoWrapper(plugin, "VAULT"));
            log.info("Hooked into zShop economy manager and registered custom economy!");
        } else {
            log.error("Could not load EconomyManager from zShop, cannot hook into it!");
        }
    }

    private record EcoWrapper(EconomyPlugin plugin, String name) implements ShopEconomy {

        @Override
        public double getMoney(OfflinePlayer offlinePlayer) {
            return plugin.economyService().getBalance(offlinePlayer.getUniqueId()).join();
        }

        @Override
        public boolean hasMoney(OfflinePlayer offlinePlayer, double v) {
            return getMoney(offlinePlayer) >= v;
        }

        @Override
        public void depositMoney(OfflinePlayer offlinePlayer, double v, String s) {
            UUID id = offlinePlayer.getUniqueId();
            plugin.economyService().deposit(id, v, "ZSHOP " + s).join();
        }

        @Override
        public void withdrawMoney(OfflinePlayer offlinePlayer, double v, String s) {
            UUID id = offlinePlayer.getUniqueId();
            plugin.economyService().withdraw(id, v, "ZSHOP " + s).join();
        }

        @Override
        public String getCurrency() {
            return "%price% Coin%s%";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDenyMessage() {
            return "&cDu hast nicht genug Coins!";
        }
    }


}
