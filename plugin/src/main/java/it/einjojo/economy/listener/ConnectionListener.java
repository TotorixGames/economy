package it.einjojo.economy.listener;

import it.einjojo.economy.CurrencyManager;
import it.einjojo.economy.OnlinePlayerEconomyCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * loads the player in the cache when they join and removes them when they leave
 */
public class ConnectionListener implements Listener {
    private final CurrencyManager currencyManager;

    public ConnectionListener(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    @EventHandler
    public void onConnect(AsyncPlayerPreLoginEvent event) {
        for (var economy : currencyManager.allEconomyServices()) {
            economy.loadBalance(event.getUniqueId());
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        for (var economy : currencyManager.allEconomyServices()) {
            var cache = economy.cache();
            if (cache == null) {
                return;
            }
            if (cache instanceof OnlinePlayerEconomyCache opec) {
                opec.unload(event.getPlayer());
            }
        }
    }
}
