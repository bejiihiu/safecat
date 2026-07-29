package kz.bejiihiu.safecat.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A provider that supplies chat metadata (prefix, suffix, display name) and can format messages.
 * Registered via {@link SafeCatRegistry#register(ChatProvider)}.
 */
public interface ChatProvider {

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
   * Returns the player's chat prefix, if any.
   *
   * @param player the player UUID
   * @return the player's chat prefix
   */
  default CompletableFuture<Optional<String>> getPrefix(UUID player) {
    return CompletableFuture.completedFuture(Optional.empty());
  }

  /**
   * Returns the player's chat suffix, if any.
   *
   * @param player the player UUID
   * @return the player's chat suffix
   */
  default CompletableFuture<Optional<String>> getSuffix(UUID player) {
    return CompletableFuture.completedFuture(Optional.empty());
  }

  /**
   * Returns the player's display name, if any.
   *
   * @param player the player UUID
   * @return the player's display name
   */
  default CompletableFuture<Optional<String>> getDisplayName(UUID player) {
    return CompletableFuture.completedFuture(Optional.empty());
  }

  /**
   * Applies custom formatting to the message for the given player.
   *
   * @param message the raw message
   * @param player the player UUID
   * @return the formatted message
   */
  default String format(String message, UUID player) {
    return message;
  }
}
