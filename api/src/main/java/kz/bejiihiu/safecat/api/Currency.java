package kz.bejiihiu.safecat.api;

import java.util.Objects;

/**
 * An immutable representation of a currency in the SafeCat economy system.
 *
 * @param id the unique identifier for this currency (e.g., "minecraft:emerald"), must not be blank
 * @param displayName the human-readable display name (e.g., "Emerald"), must not be blank
 * @param symbol the short symbol (e.g., "$", "€", "E")
 * @param type the category of this currency
 */
public record Currency(String id, String displayName, String symbol, CurrencyType type) {

  /**
   * Compact canonical constructor with null and blank validation.
   *
   * @throws NullPointerException if any parameter is null
   * @throws IllegalArgumentException if id or displayName is blank
   */
  public Currency {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(displayName, "displayName must not be null");
    Objects.requireNonNull(symbol, "symbol must not be null");
    Objects.requireNonNull(type, "type must not be null");
    if (id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (displayName.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
  }
}
