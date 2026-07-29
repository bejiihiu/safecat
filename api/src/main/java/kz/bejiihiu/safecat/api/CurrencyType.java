package kz.bejiihiu.safecat.api;

/** Represents the type or category of a currency in the SafeCat economy system. */
public enum CurrencyType {

  /** Traditional fiat currency (e.g., USD, EUR, RUB). */
  FIAT,

  /** Digital/crypto native currency (e.g., Bitcoin, Ethereum). */
  DIGITAL,

  /** In-game token or currency (e.g., guild coins, event tokens). */
  TOKEN,

  /** Custom or plugin-defined currency. */
  CUSTOM;

  public static CurrencyType safeValueOf(String name) {
    try {
      return valueOf(name);
    } catch (IllegalArgumentException e) {
      return CUSTOM;
    }
  }
}
