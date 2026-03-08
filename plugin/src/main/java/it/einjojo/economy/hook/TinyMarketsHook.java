package it.einjojo.economy.hook;

import it.einjojo.economy.EconomyPlugin;
import it.einjojo.economy.vault.ReasonProvider;
import it.einjojo.economy.vault.VaultEconomy;
import me.netizdendev.tinyMarkets.api.TinyMarketsAPI;
import me.netizdendev.tinyMarkets.api.events.ShopPrePurchaseEvent;
import me.netizdendev.tinyMarkets.api.events.ShopPreSellEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Injects the custom vault economy service.
 * Expects that everything is executed in sync.
 * <p>
 * Fire PreSellEvent -> Take ShopOwner money -> Give Seller money -> Fire ShopSellEvent
 * <p>
 * Fire PrePurchaseEvent -> Take Buyer money -> Give ShopOwner money -> Fire ShopPurchaseEvent
 */
public class TinyMarketsHook implements ReasonProvider, Listener {
    private final EconomyPlugin plugin;
    private UUID shopOwner; // the owner of the shop
    private String shopOwnerName; // the name of the owner of the shop
    private UUID actionPlayer; // buyer or seller
    private String actionPlayerName; // buyer or seller name
    private String itemName; // the material name of the item involved in the transaction
    private TransactionType lastEventType = TransactionType.UNKNOWN; // to determine if the last event was a buy or sell event


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

    @EventHandler(priority = EventPriority.MONITOR)
    public void purchaseEvent(ShopPrePurchaseEvent event) {
        if (event.isCancelled()) {
            return;
        }


    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void sellEvent(ShopPreSellEvent event) {
        if (event.isCancelled()) {
            return;
        }


    }

    @Override
    public String getReason(UUID uuid, double amount) {
        if (!uuid.equals(this.actionPlayer) && !uuid.equals(this.shopOwner)) {
            return "TINYMARKETS UNKNOWN";

        }
        StringBuilder reasonBuilder = new StringBuilder();
        reasonBuilder.append("TINYMARKETS ");
        reasonBuilder.append(lastEventType.name());
        reasonBuilder.append(" ");
        if (lastEventType == TransactionType.BUY) {
            if (uuid.equals(this.actionPlayer)) {
                reasonBuilder.append(actionPlayerName);
                reasonBuilder.append(" bought ");
                reasonBuilder.append(itemName);
                reasonBuilder.append(" from ");
                reasonBuilder.append(shopOwnerName);
            } else {
                reasonBuilder.append(shopOwnerName);
                reasonBuilder.append(" sold ");
                reasonBuilder.append(itemName);
                reasonBuilder.append(" to ");
                reasonBuilder.append(actionPlayerName);
            }
        } else if (lastEventType == TransactionType.SELL) {
            if (uuid.equals(this.actionPlayer)) {
                reasonBuilder.append(actionPlayerName);
                reasonBuilder.append(" sold ");
                reasonBuilder.append(itemName);
                reasonBuilder.append(" to ");
                reasonBuilder.append(shopOwnerName);
            } else {
                reasonBuilder.append(shopOwnerName);
                reasonBuilder.append(" bought ");
                reasonBuilder.append(itemName);
                reasonBuilder.append(" from ");
                reasonBuilder.append(actionPlayerName);
            }
        } else {
            reasonBuilder.append("UNKNOWN");
        }
        String reason = reasonBuilder.toString();
        plugin.getSLF4JLogger().info("{} generated reason: {}", this, reason);
        return reason;

    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("TinyMarkets") != null;
    }

    enum TransactionType {
        BUY,
        SELL,
        UNKNOWN
    }

    @Override
    public String toString() {
        return "TinyMarketsHook{" +
                ", shopOwner=" + shopOwner +
                ", shopOwnerName='" + shopOwnerName + '\'' +
                ", actionPlayer=" + actionPlayer +
                ", actionPlayerName='" + actionPlayerName + '\'' +
                ", itemName='" + itemName + '\'' +

                '}';
    }
}
