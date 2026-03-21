![Economy API](title.png)


Often I find myself setting up a custom economy on a minecraft server. Thus, I needed a solution that always fits. This is a java-library that manages an economic balance assigned to UUID.
It supports float transactions and is multi-instance compatible.

This project consists of two modules:
- **`plugin`** — A Paper plugin (`EconomyProviderPlugin`) that wires everything together and registers services via the Bukkit Service API.
- **`api`** — A library module exposing the interfaces and data types you use in your own plugins.

Javadocs: [https://javadocs.einjojo.it/economy](https://javadocs.einjojo.it/economy/)

---

# Using the API on a Paper Server

The plugin registers the following services via Bukkit's `ServicesManager`:

| Service class | Description |
|---|---|
| `it.einjojo.economy.EconomyService` | Async economy operations (deposit, withdraw, set balance, …) |
| `it.einjojo.economy.EconomyCache` | Read-only cache for online players |
| `net.milkbowl.vault.economy.Economy` | Vault economy implementation (only when Vault is present) |

### 1. Add the API dependency

Add the `api` artifact as a `compileOnly` dependency. The plugin provides it at runtime on the server.

**Gradle (Kotlin DSL)**
```kotlin
repositories {
    maven {
        name = "einjojoReleases"
        url = uri("https://repo.einjojo.it/releases")
    }
}

dependencies {
    compileOnly("it.einjojo:api:2.1.0-SNAPSHOT")
}
```

**Maven**
```xml
<repository>
    <id>einjojo-releases</id>
    <url>https://repo.einjojo.it/releases</url>
</repository>

<dependency>
    <groupId>it.einjojo</groupId>
    <artifactId>api</artifactId>
    <version>2.1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### 2. Declare the plugin dependency

In your `paper-plugin.yml` (or `plugin.yml`) declare `EconomyProviderPlugin` as a dependency so that it is loaded before your plugin:

```yaml
# paper-plugin.yml
dependencies:
  server:
    EconomyProviderPlugin:
      load: BEFORE
      required: true
```

### 3. Obtain the service

Use Bukkit's `ServicesManager` to retrieve the registered `EconomyService` instance:

```java
import it.einjojo.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<EconomyService> rsp =
        Bukkit.getServicesManager().getRegistration(EconomyService.class);

if (rsp == null) {
    // EconomyProviderPlugin is not loaded — handle gracefully
    throw new IllegalStateException("EconomyService is not available!");
}

EconomyService economy = rsp.getProvider();
```

A common pattern is to retrieve and store the service once during `onEnable`:

```java
public class MyPlugin extends JavaPlugin {

    private EconomyService economy;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<EconomyService> rsp =
                Bukkit.getServicesManager().getRegistration(EconomyService.class);
        if (rsp == null) {
            getLogger().severe("EconomyProviderPlugin not found! Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        economy = rsp.getProvider();
    }
}
```

### 4. Use the API

All operations that touch the database are asynchronous and return a `CompletableFuture`. **Never** call `.join()` or `.get()` on the main server thread.

#### Check balance
```java
// Instant lookup for online players (from cache)
double balance = economy.getBalanceByOnlinePlayer(player); // org.bukkit.entity.Player

// Async lookup (cache-aware; falls back to database)
economy.getBalance(player.getUniqueId()).thenAccept(bal ->
    player.sendMessage("Your balance: " + bal)
);
```

#### Deposit
```java
economy.deposit(player.getUniqueId(), 100.0, "quest-reward")
    .thenAccept(result -> {
        if (result.status() == TransactionStatus.SUCCESS) {
            player.sendMessage("You received 100 coins!");
        }
    });
```

#### Withdraw
```java
economy.withdraw(player.getUniqueId(), 50.0, "shop-purchase")
    .thenAccept(result -> {
        switch (result.status()) {
            case SUCCESS -> player.sendMessage("Purchase successful!");
            case INSUFFICIENT_FUNDS -> player.sendMessage("Not enough coins.");
            default -> player.sendMessage("Transaction failed: " + result.status());
        }
    });
```

#### Set balance
```java
economy.setBalance(player.getUniqueId(), 0.0, "admin-reset")
    .thenAccept(result -> {
        if (result.status() == TransactionStatus.SUCCESS) {
            player.sendMessage("Balance reset to 0.");
        }
    });
```

#### Check account existence
```java
economy.hasAccount(player.getUniqueId())
    .thenAccept(exists -> {
        if (!exists) {
            // first deposit will create the account automatically
        }
    });
```

### TransactionResult

Every mutating operation returns a `CompletableFuture<TransactionResult>`. The result carries:

| Field | Type | Description |
|---|---|---|
| `status()` | `TransactionStatus` | Outcome of the transaction |
| `newBalance()` | `Optional<Double>` | New balance on `SUCCESS` |
| `change()` | `double` | The amount added or subtracted |

**TransactionStatus values:**

| Status | Meaning |
|---|---|
| `SUCCESS` | Operation completed successfully |
| `INSUFFICIENT_FUNDS` | Player did not have enough funds |
| `ACCOUNT_NOT_FOUND` | No account exists for the player |
| `INVALID_AMOUNT` | Amount was zero or negative |
| `FAILED_CONCURRENCY` | Optimistic lock failed after all retries |
| `ERROR` | Unexpected error (see server logs) |

---

# Core Principles

- **Database as the Single Source of Truth:** PostgreSQL holds the definitive balance for each UUID. All other systems (caches, notifications) derive their state from it.

- **Asynchronous Operations:** No blocking database or network I/O occurs on the calling thread (e.g. the Minecraft main server thread). All API methods involving I/O return `CompletableFuture`.

- **Optimistic Concurrency Control:** Simultaneous updates from different instances are handled via optimistic locking at the database level — no distributed locks required.

- **Decoupled Notifications:** Redis Pub/Sub broadcasts balance changes after they are successfully committed to the database, allowing other instances to update their local caches.

- **Atomicity via Database:** Atomic SQL operations (`UPDATE ... SET balance = balance + ?`) ensure individual operations are applied correctly.

_Engineered using Gemini 2.5 Pro_

---

##### Multi-Instance Synchronization Flow

    Instance A wants to withdraw 10 from Player P (Current Balance: 50, Version: 5).

    Instance B wants to withdraw 20 from Player P concurrently.

    Instance A reads: Balance=50, Version=5. Checks funds (50 >= 10). Proceeds.

    Instance B reads: Balance=50, Version=5. Checks funds (50 >= 20). Proceeds.

    Instance A executes UPDATE ... SET balance = 40, version = 6 WHERE uuid = P AND version = 5. It affects 1 row (Success). Commits.

    Instance A publishes {"uuid": P, "newBalance": 40, "change": -10} to Redis.

    Instance B executes UPDATE ... SET balance = 30, version = 6 WHERE uuid = P AND version = 5. It affects 0 rows because the version is now 6 in the DB. Rolls back.

    Instance B detects the version conflict and retries.

    Instance B reads: Balance=40, Version=6. Checks funds (40 >= 20). Proceeds.

    Instance B executes UPDATE ... SET balance = 20, version = 7 WHERE uuid = P AND version = 6. It affects 1 row (Success). Commits.

    Instance B publishes {"uuid": P, "newBalance": 20, "change": -20} to Redis.


    Other Instances (C, D, etc.): Subscribe to the Redis channel. When they receive the messages from A and B, they know the confirmed balance changes and can update their local state/UI accordingly without hitting the database themselves just for notification.


**Full Changelog**: https://github.com/EinJOJO/economy/compare/1.6.1...2.0.0
