package kz.bejiihiu.safecat.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe static entry point for the SafeCat economy API.
 *
 * <p>Consumers obtain the singleton via {@link #getInstance()} and use it to query balances,
 * perform transactions, and look up currencies. The implementation is set by SafeCatCore during
 * initialisation.
 */
public interface SafeCatAPI {

  /**
   * Returns the singleton {@code SafeCatAPI} instance.
   *
   * @return the API instance
   * @throws IllegalStateException if the API has not been initialised yet
   */
  static SafeCatAPI getInstance() {
    var instance = Holder.INSTANCE;
    if (instance == null) {
      var ex = new IllegalStateException("SafeCatAPI has not been initialised yet");
      LoggerFactory.getLogger(SafeCatAPI.class)
          .error("SafeCatAPI.getInstance() called before initialisation", ex);
      throw ex;
    }
    return instance;
  }

  /**
   * Retrieves the balance of the given player for the specified currency.
   *
   * @param player the player's UUID
   * @param currencyId the currency identifier
   * @return a future that completes with the player's balance
   */
  CompletableFuture<BigDecimal> getBalance(UUID player, String currencyId);

  /**
   * Withdraws the given amount from the player's balance in the specified currency.
   *
   * @param player the player's UUID
   * @param currencyId the currency identifier
   * @param amount the amount to withdraw
   * @param reason the reason for the withdrawal
   * @return a future that completes with the transaction result
   */
  CompletableFuture<TransactionResult> withdraw(
      UUID player, String currencyId, BigDecimal amount, EventReason reason);

  /**
   * Deposits the given amount to the player's balance in the specified currency.
   *
   * @param player the player's UUID
   * @param currencyId the currency identifier
   * @param amount the amount to deposit
   * @param reason the reason for the deposit
   * @return a future that completes with the transaction result
   */
  CompletableFuture<TransactionResult> deposit(
      UUID player, String currencyId, BigDecimal amount, EventReason reason);

  /**
   * Retrieves a registered currency by its identifier.
   *
   * @param id the currency identifier
   * @return the currency, or {@code null} if not found
   */
  Currency getCurrency(String id);

  /**
   * Returns an immutable snapshot of all registered currencies.
   *
   * @return a set of all registered currencies
   */
  Set<Currency> getCurrencies();

  /**
   * Registers a command provider with the SafeCat system.
   *
   * @param command the command to register
   */
  void registerCommand(CommandProvider command);

  /**
   * Retrieves the chat prefix for the given player. Delegates to registered {@link ChatProvider}s
   * in priority order.
   *
   * @param player the player's UUID
   * @return a future that completes with the player's prefix, if any
   */
  CompletableFuture<Optional<String>> getPrefix(UUID player);

  /**
   * Retrieves the chat suffix for the given player. Delegates to registered {@link ChatProvider}s
   * in priority order.
   *
   * @param player the player's UUID
   * @return a future that completes with the player's suffix, if any
   */
  CompletableFuture<Optional<String>> getSuffix(UUID player);

  /**
   * Retrieves the display name for the given player. Delegates to registered {@link ChatProvider}s
   * in priority order.
   *
   * @param player the player's UUID
   * @return a future that completes with the player's display name, if any
   */
  CompletableFuture<Optional<String>> getDisplayName(UUID player);

  /**
   * Formats a chat message for the given player. Fires a {@link ChatFormatEvent} before delegating
   * to registered {@link ChatProvider}s in priority order.
   *
   * @param message the raw message
   * @param player the player's UUID
   * @return the formatted message
   */
  String format(String message, UUID player);

  /**
   * Returns an immutable snapshot of all registered commands.
   *
   * @return a collection of all registered command providers
   */
  Collection<CommandProvider> getCommands();

  /**
   * Returns SafeCat's internal event bus. Consumers can subscribe to events directly without
   * depending on any platform event system.
   *
   * @return the event bus
   */
  SafeCatEventBus getEventBus();

  /**
   * Checks whether the given player has the specified permission.
   *
   * @param player the player's UUID
   * @param permission the permission node to check
   * @return a future that completes with true if the player has the permission
   */
  CompletableFuture<Boolean> hasPermission(UUID player, String permission);

  /**
   * Checks whether the given player has the specified permission in a specific context.
   *
   * @param player the player's UUID
   * @param permission the permission node to check
   * @param context the context (e.g., world, dimension), or null
   * @return a future that completes with true if the player has the permission
   */
  CompletableFuture<Boolean> hasPermission(UUID player, String permission, String context);

  /**
   * Registers an adapter object. The object may implement any combination of {@link
   * CurrencyProvider}, {@link PermissionProvider}, {@link ChatProvider}, and/or {@link
   * CommandProvider} — each implemented interface is registered automatically.
   *
   * <p>This is the primary entry point for user-written adapters. Instead of using {@link
   * java.util.ServiceLoader} or subscribing to events, an adapter simply calls this method in its
   * constructor or mod initializer:
   *
   * <pre>{@code
   * public class MyAdapter extends BaseCurrencyAdapter {
   *   public MyAdapter() {
   *     SafeCatAPI.getInstance().registerAdapter(this);
   *   }
   *   // ... implement getBalance, withdraw, deposit
   * }
   * }</pre>
   *
   * @param adapter the adapter object to register
   */
  void registerAdapter(Object adapter);

  /**
   * Sets the API implementation. Intended to be called once by SafeCatCore during startup.
   *
   * @param api the implementation to set
   * @throws IllegalStateException if the API has already been set
   */
  static void setInstance(SafeCatAPI api) {
    synchronized (Holder.class) {
      if (Holder.INSTANCE != null) {
        throw new IllegalStateException(
            "SafeCatAPI has already been initialised — existing impl: "
                + Holder.INSTANCE.getClass().getName());
      }
      Holder.INSTANCE = api;
    }
  }

  /**
   * Inner class that holds the singleton reference. Using a separate class guarantees thread-safe
   * lazy initialisation without synchronisation overhead on reads.
   */
  // Package-private for testing only.
  static void resetInstance() {
    Holder.INSTANCE = null;
  }

  final class Holder {
    private static volatile SafeCatAPI INSTANCE;

    private Holder() {}
  }
}
