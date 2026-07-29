package kz.bejiihiu.safecat.api;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A non-cancelable event fired after a player-to-player (or system-to-player) transaction
 * completes. This event is informational only — the transaction has already been applied.
 */
public class TransactionEvent extends Event {

  private final UUID from;
  private final UUID to;
  private final String currencyId;
  private final BigDecimal amount;
  private final EventReason reason;
  private final TransactionResult result;

  /**
   * Creates a new transaction event.
   *
   * @param from the sender's UUID (may be a zero-UUID for system-originated transactions)
   * @param to the recipient's UUID
   * @param currencyId the currency identifier
   * @param amount the amount transferred
   * @param reason the reason for the transaction
   * @param result the outcome of the transaction
   */
  public TransactionEvent(
      UUID from,
      UUID to,
      String currencyId,
      BigDecimal amount,
      EventReason reason,
      TransactionResult result) {
    this.from = Objects.requireNonNull(from, "from");
    this.to = Objects.requireNonNull(to, "to");
    this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
    this.amount = Objects.requireNonNull(amount, "amount");
    this.reason = Objects.requireNonNull(reason, "reason");
    this.result = Objects.requireNonNull(result, "result");
  }

  /**
   * Returns the sender's UUID.
   *
   * @return the sender
   */
  public UUID getFrom() {
    return from;
  }

  /**
   * Returns the recipient's UUID.
   *
   * @return the recipient
   */
  public UUID getTo() {
    return to;
  }

  /**
   * Returns the currency identifier.
   *
   * @return the currency id
   */
  public String getCurrencyId() {
    return currencyId;
  }

  /**
   * Returns the amount transferred.
   *
   * @return the amount
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * Returns the reason for the transaction.
   *
   * @return the event reason
   */
  public EventReason getReason() {
    return reason;
  }

  /**
   * Returns the result of the transaction.
   *
   * @return the transaction result
   */
  public TransactionResult getResult() {
    return result;
  }
}
