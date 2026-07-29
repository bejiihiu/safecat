package kz.bejiihiu.safecat.api;

import java.util.Objects;

/** Fired during initialisation to allow registering {@link Currency} types. */
public class RegisterCurrenciesEvent extends Event {

  private final SafeCatRegistry registry;

  /**
   * Creates a new register currencies event.
   *
   * @param registry the registry to register currencies with
   */
  public RegisterCurrenciesEvent(SafeCatRegistry registry) {
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
   * Convenience shortcut for {@code getRegistry().register(currency)}.
   *
   * @param currency the currency to register
   */
  public void register(Currency currency) {
    registry.register(currency);
  }
}
