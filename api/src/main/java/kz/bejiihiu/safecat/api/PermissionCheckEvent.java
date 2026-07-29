package kz.bejiihiu.safecat.api;

import java.util.Objects;
import java.util.UUID;

/**
 * A cancelable event fired when a permission check is performed. Set a result via {@link
 * #setResult} to override the default permission resolution.
 */
public class PermissionCheckEvent extends Event {

  private final UUID player;
  private final String permission;
  private final String context;
  private Boolean result;

  /**
   * Creates a new permission check event.
   *
   * @param player the player being checked
   * @param permission the permission node
   * @param context optional context string (e.g. world name), may be null
   */
  public PermissionCheckEvent(UUID player, String permission, String context) {
    this.player = Objects.requireNonNull(player, "player");
    this.permission = Objects.requireNonNull(permission, "permission");
    this.context = context;
  }

  /**
   * Returns the player UUID.
   *
   * @return the player UUID
   */
  public UUID getPlayer() {
    return player;
  }

  /**
   * Returns the permission node being checked.
   *
   * @return the permission node
   */
  public String getPermission() {
    return permission;
  }

  /**
   * Returns the context string, or null.
   *
   * @return the context string, or null
   */
  public String getContext() {
    return context;
  }

  /**
   * Returns the overridden result, or null if not set.
   *
   * @return the overridden result, or null
   */
  public Boolean getResult() {
    return result;
  }

  /**
   * Overrides the permission result instead of resolving normally.
   *
   * @param result the permission result to return
   */
  public void setResult(boolean result) {
    this.result = result;
  }
}
