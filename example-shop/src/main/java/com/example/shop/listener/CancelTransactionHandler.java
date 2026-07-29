package com.example.shop.listener;

import java.util.Set;
import java.util.UUID;
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.SafeCatAPI;

/**
 * Listener that cancels balance changes for players in a PVP zone.
 *
 * <p>Subscribes directly to SafeCat's internal {@link kz.bejiihiu.safecat.api.EventBus} rather than
 * the Forge event bus bridge. This avoids fragile reflection and works identically on any platform.
 *
 * <p>Cancelling the event (via {@code setCancelled(true)}) prevents the transaction — the shop
 * command will see a failed {@code TransactionResult} and can react accordingly.
 */
public class CancelTransactionHandler {

  // Mock: real mod would query a protection plugin.
  private static final Set<UUID> PVP_ZONE_PLAYERS = Set.of();

  // Don't call SafeCatAPI.getInstance() in constructor — ExampleShop may load before Safecat.
  public CancelTransactionHandler() {}

  public static void registerLater() {
    SafeCatAPI.getInstance()
        .getEventBus()
        .on(
            BalanceChangeEvent.class,
            event -> {
              if (isInPvpZone(event.getPlayer())) {
                event.setCancelled(true);
              }
            });
  }

  private static boolean isInPvpZone(UUID playerId) {
    // Stub: replace with real protection-plugin lookup (e.g. WorldGuard, FTB Chunks).
    return PVP_ZONE_PLAYERS.contains(playerId);
  }
}
