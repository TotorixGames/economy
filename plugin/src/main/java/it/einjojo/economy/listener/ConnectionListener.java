package it.einjojo.economy.listener;

import it.einjojo.economy.EconomyService;
import it.einjojo.economy.OnlinePlayerEconomyCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * loads the player in the cache when they join and removes them when they leave
 */
public class ConnectionListener implements Listener {
    private final EconomyService economyService;
    private final OnlinePlayerEconomyCache cache;

    public ConnectionListener(EconomyService economyService, OnlinePlayerEconomyCache cache) {
        this.economyService = economyService;
        this.cache = cache;
    }

    @EventHandler
    public void onConnect(AsyncPlayerPreLoginEvent event) {
        economyService.loadBalance(event.getUniqueId());
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        cache.unload(event.getPlayer());
    }
}
