package it.einjojo.economy;

import java.util.UUID;

/**
 * Read-only cache for economy-related data.
 */
public interface EconomyCache {

    /**
     * Retrieve the cached account balance.
     *
     * @param playerUuid Player UUID
     * @return the cached balance or 0.0
     */
    double getBalance(UUID playerUuid);

    /**
     * Check if a player is cached.
     *
     * @param playerUuid player UUID
     * @return true if the player is cached, false otherwise
     */
    boolean isCached(UUID playerUuid);

    /**
     * When passed to {@link EconomyCache}, this cache will be written to
     * for all operations.
     *
     * @param playerUuid UUID of the player
     * @param balance    the new account balance
     */
    void cacheBalance(UUID playerUuid, double balance);

}