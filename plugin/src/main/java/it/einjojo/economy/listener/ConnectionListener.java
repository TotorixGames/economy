package it.einjojo.economy.listener;

import it.einjojo.economy.EconomyService;

/**
 * loads the player in the cache when they join and removes them when they leave
 */
public class ConnectionListener {

    private final EconomyService economyService;

    public ConnectionListener(EconomyService economyService) {
        this.economyService = economyService;
    }
}
