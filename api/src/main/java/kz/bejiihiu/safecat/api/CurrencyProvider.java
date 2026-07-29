package kz.bejiihiu.safecat.api;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A provider that handles balance operations for a specific currency. Implementations are
 * discovered and registered via {@link SafeCatRegistry}.
 *
 * <p>This is the V1 API. When V2 ships, this interface will be annotated {@code @Deprecated} and a
 * bridge adapter ({@code safecat-bridge-1-to-2}) will keep V1 providers working for 3 major
 * versions.
 *
 * <p>Deprecation timeline:
 *
 * <ul>
 *   <li>V2 released &rarr; {@code @Deprecated} on this interface
 *   <li>V3 released &rarr; {@code @Deprecated(forRemoval=true)} on this interface
 *   <li>V4 released &rarr; this interface removed, bridge artifact deleted
 * </ul>
 *
 * <p>Compatibility guarantee: a provider compiled for V1 works unmodified on SafeCat V2, V3, and
 * V4. See {@code docs/dev/migration-v1-to-v2.md}.
 */
public interface CurrencyProvider {

  /**
   * Returns the unique currency identifier that this provider handles.
   *
   * @return the currency id (e.g., "minecraft:emerald")
   */
  String getCurrencyId();

  /**
   * Returns the resolution priority for this provider. Providers with higher priority are consulted
   * first when resolving a currency.
   *
   * @return the priority value (higher = earlier resolution)
   */
  default int priority() {
    return 0;
  }

  /**
   * Initialises this provider with the given registry. Implementations should register their {@link
   * Currency} and any required hooks here.
   *
   * @param registry the registry to register with
   */
  void init(SafeCatRegistry registry);

  /**
   * Retrieves the current balance for the given player.
   *
   * @param player the player's UUID
   * @return a future that completes with the player's balance
   */
  CompletableFuture<BigDecimal> getBalance(UUID player);

  /**
   * Withdraws the specified amount from the player's balance.
   *
   * @param player the player's UUID
   * @param amount the amount to withdraw
   * @param reason the reason for the withdrawal
   * @return a future that completes with the transaction result
   */
  CompletableFuture<TransactionResult> withdraw(UUID player, BigDecimal amount, EventReason reason);

  /**
   * Deposits the specified amount to the player's balance.
   *
   * @param player the player's UUID
   * @param amount the amount to deposit
   * @param reason the reason for the deposit
   * @return a future that completes with the transaction result
   */
  CompletableFuture<TransactionResult> deposit(UUID player, BigDecimal amount, EventReason reason);

  /**
   * Checks whether this provider supports the given player. Override to restrict providers to
   * specific players (e.g., permission-based).
   *
   * @param player the player's UUID
   * @return true if this provider can handle the player
   */
  default boolean supports(UUID player) {
    return true;
  }
}
