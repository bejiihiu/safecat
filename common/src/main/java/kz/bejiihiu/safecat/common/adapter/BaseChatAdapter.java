package kz.bejiihiu.safecat.common.adapter;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.ChatProvider;
import kz.bejiihiu.safecat.api.SafeCatRegistry;

/**
 * skeletal {@link ChatProvider} — override whatever you need, the rest returns empty/noop.
 *
 * <p>all 4 optional methods ({@link #getPrefix}, {@link #getSuffix}, {@link #getDisplayName},
 * {@link #format}) already have defaults that do nothing, so you only override what your target mod
 * actually provides.
 *
 * <p>usage:
 *
 * <pre>{@code
 * public class MyChatAdapter extends BaseChatAdapter {
 *   public MyChatAdapter() {
 *     SafeCatAPI.getInstance().registerAdapter(this);
 *   }
 *
 *   @Override
 *   public CompletableFuture<Optional<String>> getPrefix(UUID player) {
 *     return SomeChatAPI.getPrefix(player);
 *   }
 * }
 * }</pre>
 */
public abstract class BaseChatAdapter implements ChatProvider {

  @Override
  public abstract void init(SafeCatRegistry registry);

  /** Derives a provider id from the class name. Override if you need a stable id. */
  @Override
  public String getProviderId() {
    return getClass().getSimpleName();
  }

  /** No prefix by default. */
  @Override
  public CompletableFuture<Optional<String>> getPrefix(UUID player) {
    return CompletableFuture.completedFuture(Optional.empty());
  }

  /** No suffix by default. */
  @Override
  public CompletableFuture<Optional<String>> getSuffix(UUID player) {
    return CompletableFuture.completedFuture(Optional.empty());
  }

  /** No display name by default. */
  @Override
  public CompletableFuture<Optional<String>> getDisplayName(UUID player) {
    return CompletableFuture.completedFuture(Optional.empty());
  }

  /** Passthrough by default. */
  @Override
  public String format(String message, UUID player) {
    return message;
  }
}
