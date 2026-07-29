package kz.bejiihiu.safecat.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A provider that resolves permission checks for players. Registered via {@link
 * SafeCatRegistry#register(PermissionProvider)}.
 */
public interface PermissionProvider {

  /**
   * Returns a unique identifier for this provider.
   *
   * @return a unique provider identifier
   */
  String getProviderId();

  /**
   * Returns priority for ordering (higher = consulted first).
   *
   * @return the priority value
   */
  default int priority() {
    return 0;
  }

  /**
   * Called during initialisation to register with the given registry.
   *
   * @param registry the registry
   */
  void init(SafeCatRegistry registry);

  /**
   * Checks whether the player has the given permission.
   *
   * @param player the player UUID
   * @param permission the permission node
   * @return a future completing with true if permitted
   */
  CompletableFuture<Boolean> hasPermission(UUID player, String permission);

  /**
   * Checks whether the player has the given permission in a specific context.
   *
   * @param player the player UUID
   * @param permission the permission node
   * @param context the context (e.g. world name), or null
   * @return a future completing with true if permitted
   */
  CompletableFuture<Boolean> hasPermission(UUID player, String permission, String context);
}
