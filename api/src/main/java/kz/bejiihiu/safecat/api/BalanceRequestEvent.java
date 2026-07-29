package kz.bejiihiu.safecat.api;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * A cancelable event fired to intercept or modify a balance lookup before it reaches the provider.
 * Set a balance via {@link #setBalance} to override the provider's value.
 */
public class BalanceRequestEvent extends Event {

  private final String currencyId;
  private final UUID player;
  private BigDecimal balance;

  /**
   * Creates a new balance request event.
   *
   * @param currencyId the currency being queried
   * @param player the player whose balance is being looked up
   */
  public BalanceRequestEvent(String currencyId, UUID player) {
    this.currencyId = Objects.requireNonNull(currencyId, "currencyId");
    this.player = Objects.requireNonNull(player, "player");
  }

  /**
   * Returns the currency identifier.
   *
   * @return the currency identifier
   */
  public String getCurrencyId() {
    return currencyId;
  }

  /**
   * Returns the player UUID.
   *
   * @return the player UUID
   */
  public UUID getPlayer() {
    return player;
  }

  /**
   * Returns the overridden balance, or null if not set.
   *
   * @return the overridden balance, or null
   */
  public BigDecimal getBalance() {
    return balance;
  }

  /**
   * Overrides the balance to return instead of the provider's value.
   *
   * @param balance the balance to return
   */
  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }
}
