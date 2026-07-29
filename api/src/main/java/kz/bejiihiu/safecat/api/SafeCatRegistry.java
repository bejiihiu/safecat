package kz.bejiihiu.safecat.api;

import java.util.Collection;
import java.util.Set;

/**
 * A registry for {@link Currency} types, {@link CurrencyProvider} implementations, and {@link
 * PermissionProvider} implementations. Used by providers during initialisation and by consumers to
 * look up currencies and check permissions. Implementations must be thread-safe.
 */
public interface SafeCatRegistry {

  /**
   * Registers a currency type with the system.
   *
   * @param currency the currency to register
   */
  void register(Currency currency);

  /**
   * Registers a currency provider with the system.
   *
   * @param provider the provider to register
   */
  void register(CurrencyProvider provider);

  /**
   * Registers a permission provider with the system.
   *
   * @param provider the provider to register
   */
  void register(PermissionProvider provider);

  /**
   * Registers a command provider with the system.
   *
   * @param command the command to register
   */
  void register(CommandProvider command);

  /**
   * Returns an immutable snapshot of all registered commands.
   *
   * @return a collection of all registered command providers
   */
  Collection<CommandProvider> getCommands();

  /**
   * Registers a chat provider with the system.
   *
   * @param provider the provider to register
   */
  void register(ChatProvider provider);

  /**
   * Retrieves a registered chat provider by its identifier.
   *
   * @param id the provider identifier to look up
   * @return the matching provider, or {@code null} if not found
   */
  ChatProvider getChatProvider(String id);

  /**
   * Returns an immutable snapshot of all registered chat providers.
   *
   * @return a collection of all registered chat providers
   */
  Collection<ChatProvider> getChatProviders();

  /**
   * Retrieves a registered currency by its unique identifier.
   *
   * @param id the currency identifier to look up
   * @return the matching currency, or {@code null} if not found
   */
  Currency getCurrency(String id);

  /**
   * Returns an immutable snapshot of all registered currencies.
   *
   * @return a set of all registered currencies
   */
  Set<Currency> getCurrencies();

  /**
   * Retrieves a registered permission provider by its unique identifier.
   *
   * @param id the provider identifier to look up
   * @return the matching provider, or {@code null} if not found
   */
  PermissionProvider getPermissionProvider(String id);

  /**
   * Returns an immutable snapshot of all registered permission providers.
   *
   * @return a collection of all registered permission providers
   */
  Collection<PermissionProvider> getPermissionProviders();

  /**
   * Registers an adapter object. Accepts any object that implements one or more of {@link
   * CurrencyProvider}, {@link PermissionProvider}, {@link ChatProvider}, or {@link CommandProvider}
   * — each implemented interface is dispatched to the corresponding {@code register()} overload.
   *
   * @param adapter the adapter object to register
   */
  void register(Object adapter);
}
