package it.einjojo.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyManager {
    private final Map<String, EconomyService> economyService = new ConcurrentHashMap<>();
    private final EconomyService defaultService;

    public CurrencyManager(EconomyService defaultService) {
        this.defaultService = defaultService;
    }

    public Optional<EconomyService> currency(String id) {
        return Optional.ofNullable(economyService.get(id));
    }

    public void registerCurrency(String id, EconomyService service) {
        economyService.put(id, service);
    }


    public EconomyService defaultService() {
        return defaultService;
    }

    public List<EconomyService> allEconomyServices() {
        return new ArrayList<>(economyService.values());
    }
}
