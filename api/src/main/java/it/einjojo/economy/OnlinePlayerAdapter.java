package it.einjojo.economy;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Adapter interface for retrieving player UUIDs from platform-specific player objects. This allows the economy system to work with different platforms without being tightly coupled to their player implementations.
 */
public interface OnlinePlayerAdapter {

    /**
     * Checks if the given player object is supported by this adapter. The player object should be an instance of the platform's player class.
     *
     * @param player the player object to check
     * @return true if the player object is supported, false otherwise
     */
    boolean supports(Object player);

    /**
     * Gets the player's UUID from the given player object.
     *
     * @param player the player object, which should be an instance of the platform's player class
     * @return the player's UUID
     * @throws IllegalArgumentException if the player object is not recognized
     */
    @NotNull UUID getUniqueId(Object player);

    class Chain implements OnlinePlayerAdapter {
        private final OnlinePlayerAdapter first;
        private final OnlinePlayerAdapter second;

        public Chain(OnlinePlayerAdapter first, OnlinePlayerAdapter second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean supports(Object player) {
            return first.supports(player) || second.supports(player);
        }

        @Override
        public @NotNull UUID getUniqueId(Object player) {
            if (first.supports(player)) {
                return first.getUniqueId(player);
            } else if (second.supports(player)) {
                return second.getUniqueId(player);
            } else {
                throw new IllegalArgumentException("Unsupported player object: " + player);
            }
        }
    }


}
