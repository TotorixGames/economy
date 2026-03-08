package it.einjojo.economy;

import java.util.Optional;

/**
 * Contains details about the result of an economy transaction.
 *
 * @param status     The status code indicating the result.
 * @param newBalance An Optional containing the account balance after the transaction, if applicable and successful.
 * @param change     The amount actually changed (positive for deposit/set, negative for withdrawal).
 */
public record TransactionResult(TransactionStatus status, Optional<Double> newBalance, double change) {

    // Convenience factory methods

    /**
     * Creates a successful TransactionResult.
     *
     * @param newBalance The new account balance after the transaction.
     * @param change     The amount that was added (positive) or removed (negative).
     * @return A TransactionResult with SUCCESS status and the specified balance.
     */
    public static TransactionResult success(double newBalance, double change) {
        return new TransactionResult(TransactionStatus.SUCCESS, Optional.of(newBalance), change);
    }

    /**
     * Creates a TransactionResult indicating insufficient funds.
     *
     * @return A TransactionResult with INSUFFICIENT_FUNDS status and no balance.
     */
    public static TransactionResult insufficientFunds() {
        return new TransactionResult(TransactionStatus.INSUFFICIENT_FUNDS, Optional.empty(), 0);
    }

    /**
     * Creates a TransactionResult indicating the account was not found.
     *
     * @return A TransactionResult with ACCOUNT_NOT_FOUND status and no balance.
     */
    public static TransactionResult accountNotFound() {
        return new TransactionResult(TransactionStatus.ACCOUNT_NOT_FOUND, Optional.empty(), 0);
    }

    /**
     * Creates a TransactionResult indicating an invalid amount.
     *
     * @return A TransactionResult with INVALID_AMOUNT status and no balance.
     */
    public static TransactionResult invalidAmount() {
        return new TransactionResult(TransactionStatus.INVALID_AMOUNT, Optional.empty(), 0);
    }

    /**
     * Creates a TransactionResult indicating a concurrency error.
     *
     * @return A TransactionResult with FAILED_CONCURRENCY status and no balance.
     */
    public static TransactionResult concurrentModification() {
        return new TransactionResult(TransactionStatus.FAILED_CONCURRENCY, Optional.empty(), 0);
    }

    /**
     * Creates a TransactionResult indicating a general error.
     *
     * @return A TransactionResult with ERROR status and no balance.
     */
    public static TransactionResult error() {
        return new TransactionResult(TransactionStatus.ERROR, Optional.empty(), 0);
    }

    /**
     * Checks whether the transaction was successful.
     *
     * @return {@code true} if the status is {@link TransactionStatus#SUCCESS}, otherwise {@code false}.
     */
    public boolean isSuccess() {
        return status == TransactionStatus.SUCCESS;
    }
}