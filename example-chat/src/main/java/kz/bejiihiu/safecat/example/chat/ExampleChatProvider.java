package kz.bejiihiu.safecat.example.chat;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import kz.bejiihiu.safecat.api.ChatProvider;
import kz.bejiihiu.safecat.api.SafeCatRegistry;

/**
 * A reference implementation of {@link ChatProvider} that stores prefixes, suffixes, and display
 * names in memory and formats messages with a green prefix.
 *
 * <p>The provider is discovered via {@link java.util.ServiceLoader} — see {@code
 * META-INF/services/kz.bejiihiu.safecat.api.ChatProvider}.
 */
public class ExampleChatProvider implements ChatProvider {

  /** Sole constructor — created by {@link java.util.ServiceLoader}. */
  public ExampleChatProvider() {}

  private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();
  private final Map<UUID, String> suffixes = new ConcurrentHashMap<>();
  private final Map<UUID, String> displayNames = new ConcurrentHashMap<>();

  /**
   * Sets the chat prefix for a player. Use {@code §} color codes.
   *
   * @param player the player UUID
   * @param prefix the prefix string
   */
  public void setPrefix(UUID player, String prefix) {
    prefixes.put(player, prefix);
  }

  /**
   * Sets the chat suffix for a player.
   *
   * @param player the player UUID
   * @param suffix the suffix string
   */
  public void setSuffix(UUID player, String suffix) {
    suffixes.put(player, suffix);
  }

  /**
   * Sets the display name for a player.
   *
   * @param player the player UUID
   * @param displayName the display name
   */
  public void setDisplayName(UUID player, String displayName) {
    displayNames.put(player, displayName);
  }

  @Override
  public String getProviderId() {
    return "example-chat";
  }

  @Override
  public void init(SafeCatRegistry registry) {
    registry.register(this);
  }

  @Override
  public CompletableFuture<Optional<String>> getPrefix(UUID player) {
    return CompletableFuture.completedFuture(Optional.ofNullable(prefixes.get(player)));
  }

  @Override
  public CompletableFuture<Optional<String>> getSuffix(UUID player) {
    return CompletableFuture.completedFuture(Optional.ofNullable(suffixes.get(player)));
  }

  @Override
  public CompletableFuture<Optional<String>> getDisplayName(UUID player) {
    return CompletableFuture.completedFuture(Optional.ofNullable(displayNames.get(player)));
  }

  @Override
  public String format(String message, UUID player) {
    // Wrap the message in green (section sign + 'a') as an example formatting.
    return "§a" + message;
  }
}
