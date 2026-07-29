package kz.bejiihiu.safecat.common.adapter;

import java.math.BigDecimal;
import java.util.UUID;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.CurrencyType;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.SafeCatRegistry;

/**
 * skeletal {@link CurrencyProvider} — override the boring stuff once and forget about it.
 *
 * <p>you still need to implement the 4 real methods:
 *
 * <ul>
 *   <li>{@link #getBalance(UUID)}
 *   <li>{@link #withdraw(UUID, BigDecimal, EventReason)}
 *   <li>{@link #deposit(UUID, BigDecimal, EventReason)}
 *   <li>{@link #getCurrencyId()}
 * </ul>
 *
 * <p>everything else ({@link #init}, {@link #priority}, {@link #supports}) is done for you.
 * override {@link #getDisplayName()} and {@link #getSymbol()} if you want a pretty currency name
 * instead of the currency id.
 *
 * <p>usage:
 *
 * <pre>{@code
 * public class MyAdapter extends BaseCurrencyAdapter {
 *   public MyAdapter() {
 *     SafeCatAPI.getInstance().registerAdapter(this);
 *   }
 *
 *   @Override
 *   public String getCurrencyId() { return "myeco:coin"; }
 *
 *   @Override
 *   public CompletableFuture<BigDecimal> getBalance(UUID player) { ... }
 *
 *   @Override
 *   public CompletableFuture<TransactionResult> withdraw(UUID player, BigDecimal amount, EventReason reason) { ... }
 *
 *   @Override
 *   public CompletableFuture<TransactionResult> deposit(UUID player, BigDecimal amount, EventReason reason) { ... }
 * }
 * }</pre>
 */
public abstract class BaseCurrencyAdapter implements CurrencyProvider {

  @Override
  public void init(SafeCatRegistry registry) {
    registry.register(
        new Currency(getCurrencyId(), getDisplayName(), getSymbol(), getCurrencyType()));
  }

  /**
   * Human-readable name for your currency. Override if you want something prettier than the
   * currency id.
   */
  protected String getDisplayName() {
    return getCurrencyId();
  }

  /**
   * Short symbol for UI display. Defaults to the first char of currency id, upper-cased. Override
   * if you want something fancy like Ⓝ or 💰.
   */
  protected String getSymbol() {
    var id = getCurrencyId();
    return id.isEmpty() ? "$" : id.substring(0, 1).toUpperCase();
  }

  /** Defaults to {@link CurrencyType#TOKEN} — override if your currency is fiat or digital. */
  protected CurrencyType getCurrencyType() {
    return CurrencyType.TOKEN;
  }
}
