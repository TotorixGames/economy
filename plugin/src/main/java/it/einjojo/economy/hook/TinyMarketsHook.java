package it.einjojo.economy.hook;

import it.einjojo.economy.EconomyPlugin;
import it.einjojo.economy.vault.ReasonProvider;
import it.einjojo.economy.vault.VaultEconomy;
import me.netizdendev.tinyMarkets.api.TinyMarketsAPI;

import java.util.UUID;

/**
 * Replaces the vault instance for the tiny markets plugin
 */
public class TinyMarketsHook implements ReasonProvider {
    private final EconomyPlugin plugin;

    public TinyMarketsHook(EconomyPlugin plugin) {
        this.plugin = plugin;
    }


    public void init() {
        TinyMarketsAPI api = TinyMarketsAPI.getInstance();
        if (api == null) {
            plugin.getSLF4JLogger().error("TinyMarkets API is not available, cannot hook into it!");
            return;
        }
        api.setEconomyProvider(new VaultEconomy(this, plugin));
        plugin.getSLF4JLogger().info("Provided custom economy!");
    }


    @Override
    public String getReason(UUID uuid, double amount) {
        return "TINYMARKETS";

    }

}
