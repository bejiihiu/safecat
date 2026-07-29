package kz.bejiihiu.safecat.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * An immutable result of a transaction, containing the outcome, amount, and a human-readable
 * message.
 *
 * @param success whether the transaction completed successfully
 * @param amount the net amount involved (may be zero for failures)
 * @param currencyId the currency this transaction operated on
 * @param message a human-readable description of the result
 * @param timestamp the instant at which this result was created
 */
public record TransactionResult(
    boolean success, BigDecimal amount, String currencyId, String message, Instant timestamp) {

  /**
   * Compact canonical constructor with null checks.
   *
   * @throws NullPointerException if amount, currencyId, message, or timestamp is null
   */
  public TransactionResult {
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(currencyId, "currencyId must not be null");
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(timestamp, "timestamp must not be null");
  }

  /**
   * Creates a successful transaction result with the current timestamp.
   *
   * @param amount the amount transferred
   * @param currencyId the currency identifier
   * @return a success result
   */
  public static TransactionResult success(BigDecimal amount, String currencyId) {
    return new TransactionResult(true, amount, currencyId, "success", Instant.now());
  }

  /**
   * Creates a failed transaction result with the current timestamp.
   *
   * @param currencyId the currency identifier
   * @param message a description of why the transaction failed
   * @return a failure result
   */
  public static TransactionResult failure(String currencyId, String message) {
    return new TransactionResult(false, BigDecimal.ZERO, currencyId, message, Instant.now());
  }
}
