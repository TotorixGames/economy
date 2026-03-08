package it.einjojo.economy.db;

import it.einjojo.economy.exception.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * SQLite implementation of the {@link EconomyRepository}.
 * Designed for local development and testing purposes.
 * All public methods execute blocking JDBC calls and are intended to be
 * invoked asynchronously by the service layer.
 */
public class SqliteEconomyRepository implements EconomyRepository {

    private static final Logger log = LoggerFactory.getLogger(SqliteEconomyRepository.class);
    private static final String DEFAULT_TABLE_PREFIX = "eco";

    private final String economyTableName;
    private final String logTableName;
    private final ConnectionProvider connectionProvider;
    private final String createTableSql;
    private final String createLogTableSql;
    private final String findAccountSql;
    private final String updateConditionalSql;
    private final String upsertSetSql;
    private final String upsertIncrementSql;
    private final String insertLogEntrySql;
    private final String getLogEntriesSql;

    /**
     * Constructs a new SqliteEconomyRepository.
     *
     * @param connectionProvider Provides database connections. Must not be null.
     */
    public SqliteEconomyRepository(ConnectionProvider connectionProvider) {
        this(connectionProvider, DEFAULT_TABLE_PREFIX);
    }

    /**
     * Constructs a new SqliteEconomyRepository.
     *
     * @param connectionProvider Provides database connections. Must not be null.
     * @param tablePrefix        The prefix of the table to use for storing account data. Must not be null or empty.
     */
    public SqliteEconomyRepository(ConnectionProvider connectionProvider, String tablePrefix) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider cannot be null");
        Objects.requireNonNull(tablePrefix, "tablePrefix cannot be null");
        if (tablePrefix.isBlank()) {
            throw new IllegalArgumentException("tablePrefix cannot be empty");
        }

        this.economyTableName = tablePrefix + "_balances";
        this.logTableName = tablePrefix + "_logs";

        // SQLite table creation SQL - no foreign keys
        createTableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0.0,
                    version INTEGER NOT NULL DEFAULT 0,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """.formatted(economyTableName);

        // SQLite log table creation SQL - no foreign keys
        createLogTableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    relative_change REAL NOT NULL,
                    reason TEXT,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid, version)
                );
                """.formatted(logTableName);

        findAccountSql = "SELECT uuid, balance, version FROM %s WHERE uuid = ?;".formatted(economyTableName);

        // SQLite UPSERT syntax (INSERT ... ON CONFLICT)
        upsertIncrementSql = """
                INSERT INTO %s (uuid, balance, version) VALUES (?, ?, 0)
                ON CONFLICT(uuid) DO UPDATE SET
                  balance = %s.balance + EXCLUDED.balance,
                  version = %s.version + 1,
                  updated_at = CURRENT_TIMESTAMP
                RETURNING balance, version;
                """.formatted(economyTableName, economyTableName, economyTableName);

        upsertSetSql = """
                INSERT INTO %s (uuid, balance, version) VALUES (?, ?, 0)
                ON CONFLICT(uuid) DO UPDATE SET
                  balance = EXCLUDED.balance,
                  version = %s.version + 1,
                  updated_at = CURRENT_TIMESTAMP
                RETURNING balance, version;
                """.formatted(economyTableName, economyTableName);

        updateConditionalSql = """
                UPDATE %s SET balance = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE uuid = ? AND version = ?;
                """.formatted(economyTableName);

        insertLogEntrySql = """
                INSERT INTO %s (uuid, version, relative_change, reason) VALUES (?, ?, ?, ?)
                """.formatted(logTableName);

        getLogEntriesSql = """
                SELECT uuid, version, relative_change, reason, created_at
                FROM %s
                WHERE uuid = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?;
                """.formatted(logTableName);
    }

    @Override
    public void init() throws RepositoryException {
        log.info("Ensuring SQLite database schema for '{}' and '{}' exists...", economyTableName, logTableName);
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            log.debug("Executing: {}", createTableSql);
            stmt.execute(createTableSql);
            log.info("Table '{}' ensured.", economyTableName);

            log.debug("Executing: {}", createLogTableSql);
            stmt.execute(createLogTableSql);
            log.info("Table '{}' ensured.", logTableName);

        } catch (SQLException e) {
            log.error("Failed to ensure database schema", e);
            throw new RepositoryException("Failed to ensure database schema", e);
        }
    }

    @Override
    public Optional<AccountData> findAccountData(UUID playerUuid) throws RepositoryException {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        log.debug("Finding account data for UUID: {}", playerUuid);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(findAccountSql)) {

            ps.setString(1, playerUuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AccountData data = new AccountData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getDouble("balance"),
                            rs.getLong("version")
                    );
                    log.debug("Found account data: {}", data);
                    return Optional.of(data);
                } else {
                    log.debug("Account data not found for UUID: {}", playerUuid);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            log.error("Failed to find account data for UUID: {}", playerUuid, e);
            throw new RepositoryException("Failed to find account data for UUID: " + playerUuid, e);
        }
    }

    @Override
    public AccountData upsertAndIncrementBalance(UUID playerUuid, double amount) throws RepositoryException {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to increment must be positive: " + amount);
        }
        log.debug("Upserting and incrementing balance for UUID: {} by amount: {}", playerUuid, amount);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(upsertIncrementSql)) {

            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, amount);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AccountData accountData = new AccountData(
                            playerUuid,
                            rs.getDouble("balance"),
                            rs.getLong("version")
                    );
                    log.debug("Upsert increment successful for UUID: {}. New data: {}", playerUuid, accountData);
                    return accountData;
                } else {
                    log.error("Upsert increment failed unexpectedly for UUID: {} (no data returned)", playerUuid);
                    throw new RepositoryException("Upsert increment failed unexpectedly for UUID: " + playerUuid + " (no data returned)", null);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to upsert/increment balance for UUID: {}", playerUuid, e);
            throw new RepositoryException("Failed to upsert/increment balance for UUID: " + playerUuid, e);
        }
    }

    @Override
    public boolean updateBalanceConditional(UUID playerUuid, double newBalance, long expectedVersion) throws RepositoryException {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        if (newBalance < 0) {
            throw new IllegalArgumentException("New balance cannot be negative: " + newBalance);
        }
        log.debug("Attempting conditional update for UUID: {} to balance: {} with expected version: {}", playerUuid, newBalance, expectedVersion);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateConditionalSql)) {

            ps.setDouble(1, newBalance);
            ps.setString(2, playerUuid.toString());
            ps.setLong(3, expectedVersion);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 1) {
                log.debug("Conditional update successful for UUID: {}", playerUuid);
                return true;
            } else {
                log.debug("Conditional update failed for UUID: {} (rows affected: {}). Expected version: {}", playerUuid, rowsAffected, expectedVersion);
                return false;
            }
        } catch (SQLException e) {
            log.error("Failed conditional update for UUID: {}", playerUuid, e);
            throw new RepositoryException("Failed conditional update for UUID: " + playerUuid, e);
        }
    }

    @Override
    public AccountData upsertAndSetBalance(UUID playerUuid, double amount) throws RepositoryException {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to set must be non-negative: " + amount);
        }
        log.debug("Upserting and setting balance for UUID: {} to amount: {}", playerUuid, amount);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(upsertSetSql)) {

            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, amount);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AccountData accountData = new AccountData(
                            playerUuid,
                            rs.getDouble("balance"),
                            rs.getLong("version")
                    );
                    log.debug("Upsert set successful for UUID: {}. New data: {}", playerUuid, accountData);
                    return accountData;
                } else {
                    log.error("Upsert set failed unexpectedly for UUID: {} (no data returned)", playerUuid);
                    throw new RepositoryException("Upsert set failed unexpectedly for UUID: " + playerUuid + " (no data returned)", null);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to upsert/set balance for UUID: {}", playerUuid, e);
            throw new RepositoryException("Failed to upsert/set balance for UUID: " + playerUuid, e);
        }
    }

    @Override
    public void createLogEntry(UUID playerUuid, long version, double relativeChange, String reason) throws RepositoryException {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        log.debug("Creating log entry for UUID: {}, Version: {}, Change: {}, Reason: {}", playerUuid, version, relativeChange, reason);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertLogEntrySql)) {

            ps.setString(1, playerUuid.toString());
            ps.setLong(2, version);
            ps.setDouble(3, relativeChange);
            ps.setString(4, reason);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 1) {
                log.debug("Log entry created successfully for UUID: {}", playerUuid);
            } else {
                log.warn("Log entry creation affected {} rows, expected 1 for UUID: {}", rowsAffected, playerUuid);
            }
        } catch (SQLException e) {
            log.error("Failed to create log entry for UUID: {}", playerUuid, e);
            throw new RepositoryException("Failed to create log entry for UUID: " + playerUuid, e);
        }
    }

    @Override
    public List<LogEntry> getLogEntries(UUID playerUuid, int limit, int page) throws RepositoryException {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive.");
        }
        if (page <= 0) {
            throw new IllegalArgumentException("Page must be positive.");
        }

        log.debug("Fetching log entries for UUID: {}, Limit: {}, Page: {}", playerUuid, limit, page);
        List<LogEntry> entries = new ArrayList<>();

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(getLogEntriesSql)) {

            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            ps.setInt(3, (page - 1) * limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LogEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getLong("version"),
                            rs.getDouble("relative_change"),
                            rs.getString("reason"),
                            Instant.parse(rs.getString("created_at").replace(" ", "T") + "Z")
                    ));
                }
            }
            log.debug("Found {} log entries for UUID: {} (Limit: {}, Page: {})", entries.size(), playerUuid, limit, page);
            return entries;
        } catch (SQLException e) {
            log.error("Failed to retrieve log entries for UUID: {}", playerUuid, e);
            throw new RepositoryException("Failed to retrieve log entries for UUID: " + playerUuid, e);
        }
    }
}
