package kz.bejiihiu.safecat.provider.numismatic;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.*;

/**
 * A reference implementation of {@link CurrencyProvider} that shows how a Minecraft economy mod
 * (here simulated as "Numismatic Overhaul") integrates with SafeCat.
 *
 * <p><b>Real-world usage:</b> Swap the in-memory {@link MemoryEconomy} for actual Numismatic API
 * calls:
 *
 * <pre>{@code
 * // build.gradle dependency:
 * //   modImplementation("curse.maven:numismatic-overhaul-123456:9876543")
 *
 * // Inside getBalance:
 * var currency = NumismaticAPI.getStorage(player);
 * return BigDecimal.valueOf(currency.getAmount());
 * }</pre>
 *
 * <p>The provider is discovered via {@link java.util.ServiceLoader} — see {@code
 * META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider}.
 */
public class NumismaticProvider implements CurrencyProvider {

  /** Sole constructor — created by {@link java.util.ServiceLoader}. */
  public NumismaticProvider() {}

  /**
   * In-memory backend. A real provider would have a reference to {@code NumismaticAPI} or a similar
   * mod singleton.
   */
  private final MemoryEconomy economy = new MemoryEconomy();

  @Override
  public String getCurrencyId() {
    // Matches the Currency record registered in init()
    return "numismatic:coin";
  }

  @Override
  public void init(SafeCatRegistry registry) {
    // 1. Register the currency so SafeCat knows about it.
    registry.register(
        new Currency(
            getCurrencyId(),
            "Numismatic Coin",
            "Ⓝ", // Ⓝ as a compact symbol
            CurrencyType.TOKEN));

    // 2. Register this provider so SafeCat routes balance calls to us.
    registry.register(this);

    // 3. Seed a starting balance (in a real mod this would read from disk).
    //    See: NumismaticAPI.getStorage(player).getAmount()
  }

  @Override
  public CompletableFuture<BigDecimal> getBalance(UUID player) {
    // Real impl:
    //   var storage = NumismaticAPI.getStorage(player);
    //   return CompletableFuture.completedFuture(
    //       BigDecimal.valueOf(storage.getAmount()));
    return CompletableFuture.completedFuture(economy.getBalance(player));
  }

  @Override
  public CompletableFuture<TransactionResult> withdraw(
      UUID player, BigDecimal amount, EventReason reason) {
    // Real impl:
    //   var storage = NumismaticAPI.getStorage(player);
    //   if (storage.getAmount() < amount.longValue()) {
    //       return failure(...);
    //   }
    //   storage.setAmount(storage.getAmount() - amount.longValue());
    //   return success(...);

    if (amount.signum() <= 0) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(getCurrencyId(), "withdrawal amount must be positive"));
    }

    if (!economy.apply(player, amount.negate())) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(getCurrencyId(), "insufficient balance"));
    }

    return CompletableFuture.completedFuture(TransactionResult.success(amount, getCurrencyId()));
  }

  @Override
  public CompletableFuture<TransactionResult> deposit(
      UUID player, BigDecimal amount, EventReason reason) {
    // Real impl:
    //   var storage = NumismaticAPI.getStorage(player);
    //   storage.setAmount(storage.getAmount() + amount.longValue());
    //   return success(...);

    if (amount.signum() <= 0) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(getCurrencyId(), "deposit amount must be positive"));
    }

    economy.apply(player, amount);

    return CompletableFuture.completedFuture(TransactionResult.success(amount, getCurrencyId()));
  }
}
