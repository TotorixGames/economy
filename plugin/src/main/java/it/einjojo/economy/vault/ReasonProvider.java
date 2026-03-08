package it.einjojo.economy.vault;

import java.util.UUID;

/**
 * Used when this plugin should provide a custom vault instance for the underlying economy plugin.
 * <p>
 * Most likely it uses the event system of the plugin to determine why a transaction will happen and will try to figure out the reason accordingly
 */
public interface ReasonProvider {
    /**
     *
     */
    ReasonProvider DEFAULT_VAULT = (uuid, amount) -> "VAULT";

    /**
     * Gets the reason for a transaction.
     *
     * @param uuid   the UUID of the player involved in the transaction
     * @param amount the amount of the transaction
     * @return a string representing the reason for the transaction
     */
    String getReason(UUID uuid, double amount);

}
