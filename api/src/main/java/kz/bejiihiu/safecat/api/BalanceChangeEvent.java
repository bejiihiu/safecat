package kz.bejiihiu.safecat.api;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A cancelable event fired before a player's balance is modified.
 *
 * <p>Cancelling this event prevents the balance change from being applied.
 */
public class BalanceChangeEvent extends Event {

  private final UUID player;
  private final String currencyId;
  private final BigDecimal amount;
  private final EventReason reason;
  private final Operation operation;

  /**
   * Creates a new balance change event.
   *
   * @param player the player whose balance is changing
   * @param currencyId the affected currency
   * @param amount the amount being added or removed
   * @param reason the reason for the change
   * @param operation whether this is a withdrawal or deposit
   */
  public BalanceChangeEvent(
      UUID player, String currencyId, BigDecimal amount, EventReason reason, Operation operation) {
    this.player = Objects.requireNonNull(player, "player");
    this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
    this.amount = Objects.requireNonNull(amount, "amount");
    this.reason = Objects.requireNonNull(reason, "reason");
    this.operation = Objects.requireNonNull(operation, "operation");
  }

  /**
   * Returns the player whose balance is changing.
   *
   * @return the player UUID
   */
  public UUID getPlayer() {
    return player;
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
   * Returns the amount of the balance change.
   *
   * @return the amount
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * Returns the reason for the balance change.
   *
   * @return the event reason
   */
  public EventReason getReason() {
    return reason;
  }

  /**
   * Returns the type of balance operation.
   *
   * @return the operation (WITHDRAW or DEPOSIT)
   */
  public Operation getOperation() {
    return operation;
  }

  /** The type of balance operation. */
  public enum Operation {

    /** Funds are being removed from the player's balance. */
    WITHDRAW,

    /** Funds are being added to the player's balance. */
    DEPOSIT
  }
}
