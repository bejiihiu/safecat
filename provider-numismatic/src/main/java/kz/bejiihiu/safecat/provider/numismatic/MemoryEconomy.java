package kz.bejiihiu.safecat.provider.numismatic;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An in-memory economy backend that simulates what the real Numismatic Overhaul API would provide.
 *
 * <p>In a real provider you'd replace this whole class with calls to {@code
 * net.numismaticoverhaul.api.NumismaticAPI}:
 *
 * <pre>{@code
 * // Real implementation:
 * var numismaticCurrency = NumismaticAPI.getCurrency(player);
 * return BigDecimal.valueOf(numismaticCurrency.getAmount());
 * }</pre>
 */
final class MemoryEconomy {

  private final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

  /** Seeds a starting balance for the given player. */
  void initPlayer(UUID player, BigDecimal amount) {
    balances.putIfAbsent(player, amount);
  }

  /** Returns the current balance, or zero if unset. */
  BigDecimal getBalance(UUID player) {
    return balances.getOrDefault(player, BigDecimal.ZERO);
  }

  /**
   * Atomically adds the given amount; amount may be negative for withdrawal. Returns false if the
   * result would go below zero.
   */
  boolean apply(UUID player, BigDecimal delta) {
    var modified = new AtomicBoolean(false);
    balances.compute(
        player,
        (id, current) -> {
          var cur = current != null ? current : BigDecimal.ZERO;
          var next = cur.add(delta);
          if (next.signum() < 0) {
            return cur;
          }
          modified.set(true);
          return next;
        });
    return modified.get();
  }
}
