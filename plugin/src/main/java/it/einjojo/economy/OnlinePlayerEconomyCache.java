package it.einjojo.economy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;

/**
 * Caches online player balances
 */
public class OnlinePlayerEconomyCache implements EconomyCache {
    public static final int CACHE_TTL_MINUTES = 10;
    private final Cache<UUID, Double> cache = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(CACHE_TTL_MINUTES)).build();

    @Override
    public double getBalance(UUID playerUuid) {
        return cache.get(playerUuid, (key) -> 0.0);
    }

    @Override
    public boolean isCached(UUID playerUuid) {
        return cache.getIfPresent(playerUuid) != null;
    }

    @Override
    public void cacheBalance(UUID playerUuid, double balance) {
        cache.put(playerUuid, balance);
    }

    public void unload(@NotNull Player player) {
        cache.invalidate(player.getUniqueId());
    }
}
