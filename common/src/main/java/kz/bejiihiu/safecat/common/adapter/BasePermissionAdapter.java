package kz.bejiihiu.safecat.common.adapter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.SafeCatRegistry;

/**
 * skeletal {@link PermissionProvider} — override only {@link #hasPermission} and optionally {@link
 * #getProviderId()}.
 *
 * <p>{@link #init} registers you automatically. the context-aware {@link #hasPermission(UUID,
 * String, String)} delegates to the context-less version by default.
 *
 * <p>usage:
 *
 * <pre>{@code
 * public class MyPermsAdapter extends BasePermissionAdapter {
 *   public MyPermsAdapter() {
 *     SafeCatAPI.getInstance().registerAdapter(this);
 *   }
 *
 *   @Override
 *   public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
 *     return LuckyPermsAPI.hasPermission(player, permission);
 *   }
 * }
 * }</pre>
 */
public abstract class BasePermissionAdapter implements PermissionProvider {

  @Override
  public abstract void init(SafeCatRegistry registry);

  /**
   * Derives a provider id from the class name ("com.example.MyPermsAdapter" → "MyPermsAdapter").
   * Override if you need a stable id.
   */
  @Override
  public String getProviderId() {
    return getClass().getSimpleName();
  }

  /**
   * Convenience: context-aware check delegates to context-less by default. Override if your
   * permission mod actually supports contexts.
   */
  @Override
  public CompletableFuture<Boolean> hasPermission(UUID player, String permission, String context) {
    return hasPermission(player, permission);
  }
}
