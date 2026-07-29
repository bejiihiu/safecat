package kz.bejiihiu.safecat.example.permissions;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.SafeCatRegistry;

/**
 * A reference implementation of {@link PermissionProvider} that stores permissions in memory and
 * supports wildcard matching.
 *
 * <p>Wildcard rules:
 *
 * <ul>
 *   <li>{@code *} matches any single permission segment (e.g. {@code group.*} matches {@code
 *       group.vip} but not {@code group.vip.admin})
 *   <li>{@code **} matches any number of segments
 * </ul>
 *
 * <p>The provider is discovered via {@link java.util.ServiceLoader} — see {@code
 * META-INF/services/kz.bejiihiu.safecat.api.PermissionProvider}.
 */
public class ExamplePermissionProvider implements PermissionProvider {

  /** Sole constructor — created by {@link java.util.ServiceLoader}. */
  public ExamplePermissionProvider() {}

  private final Map<UUID, Set<String>> permissions = new ConcurrentHashMap<>();

  /**
   * Grants a permission to the given player.
   *
   * @param player the player UUID
   * @param permission the permission node (may contain wildcards)
   */
  public void grant(UUID player, String permission) {
    permissions.computeIfAbsent(player, k -> new CopyOnWriteArraySet<>()).add(permission);
  }

  /**
   * Revokes a permission from the given player.
   *
   * @param player the player UUID
   * @param permission the permission node to revoke
   */
  public void revoke(UUID player, String permission) {
    permissions.computeIfAbsent(player, k -> new CopyOnWriteArraySet<>()).remove(permission);
  }

  @Override
  public String getProviderId() {
    return "example-permissions";
  }

  @Override
  public void init(SafeCatRegistry registry) {
    registry.register(this);
  }

  @Override
  public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
    Set<String> granted = permissions.get(player);
    if (granted == null) {
      return CompletableFuture.completedFuture(false);
    }
    // Exact match first, then wildcard.
    if (granted.contains(permission)) {
      return CompletableFuture.completedFuture(true);
    }
    for (String pattern : granted) {
      if (wildcardMatch(pattern, permission)) {
        return CompletableFuture.completedFuture(true);
      }
    }
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public CompletableFuture<Boolean> hasPermission(UUID player, String permission, String context) {
    // This example ignores context. A real provider could incorporate world/dimension.
    return hasPermission(player, permission);
  }

  static boolean wildcardMatch(String pattern, String permission) {
    String regex =
        Arrays.stream(pattern.split("\\."))
            .map(
                s ->
                    switch (s) {
                      case "**" -> ".*";
                      case "*" -> "[^.]+";
                      default -> Pattern.quote(s);
                    })
            .collect(Collectors.joining("\\.", "^", "$"));
    return permission.matches(regex);
  }
}
