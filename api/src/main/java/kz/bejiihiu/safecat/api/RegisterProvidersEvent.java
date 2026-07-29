package kz.bejiihiu.safecat.api;

import java.util.Objects;

/**
 * Fired during initialisation to allow registering {@link CurrencyProvider} and {@link
 * PermissionProvider} implementations.
 */
public class RegisterProvidersEvent extends Event {

  private final SafeCatRegistry registry;

  /**
   * Creates a new register providers event.
   *
   * @param registry the registry to register providers with
   */
  public RegisterProvidersEvent(SafeCatRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  /**
   * Returns the registry.
   *
   * @return the registry
   */
  public SafeCatRegistry getRegistry() {
    return registry;
  }

  /**
   * Convenience shortcut for {@code getRegistry().register(provider)}.
   *
   * @param provider the currency provider to register
   */
  public void register(CurrencyProvider provider) {
    registry.register(provider);
  }

  /**
   * Convenience shortcut for {@code getRegistry().register(provider)}.
   *
   * @param provider the permission provider to register
   */
  public void register(PermissionProvider provider) {
    registry.register(provider);
  }
}
