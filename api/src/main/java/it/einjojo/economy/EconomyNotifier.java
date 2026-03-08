package it.einjojo.economy;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Notifies about economy updates.
 */
public interface EconomyNotifier {
    /**
     * @param playerUuid Betroffener Spieler
     * @param balance    der neue Kontostand
     * @param amount     relative Änderung
     */
    void publishUpdate(@NotNull UUID playerUuid, double balance, double amount);
}