# Permissions

SafeCat routes permission checks through registered `PermissionProvider`s via `SafeCatAPI.hasPermission()`.
Without any `PermissionProvider`, the API returns `false` for all checks, and only the Brigadier-level
op gate applies (see below).

## How Permission Checks Work

Commands registered through `CommandProvider` have two layers of access control:

1. **Brigadier op gate** — if `CommandProvider.getPermission()` returns a non-null string,
   the command requires Minecraft op level 2 (`source.hasPermission(2)`) to even appear in tab
   completion. If it returns null, the command is visible to everyone.

2. **PermissionProvider chain** — when a command executes, `CommandSender.hasPermission()` calls
   `SafeCatAPI.hasPermission()`, which fires a `PermissionCheckEvent` then iterates registered
   `PermissionProvider`s in priority order (highest first). The first provider that returns `true`
   grants access. If all return `false`, access is denied.

This means a command with `getPermission() == null` (like `/safecat`) has no gate at all.
A command with `getPermission() == "myshop.buy"` is gated by both op level 2 and any
registered permission plugins.

## Built-in Commands

| Command | Permission Node | Default | Notes |
|---------|----------------|---------|-------|
| `/safecat` | none | everyone | `getPermission()` returns null, no gate |
| `/safecat help` | none | everyone | handled by `/safecat` |
| `/safecat status` | none | everyone | handled by `/safecat` |
| `/safecat currencies` | none | everyone | handled by `/safecat` |

## Custom Commands (from providers)

Commands registered via `CommandProvider` use the permission string returned by
`CommandProvider.getPermission()`. Admins can grant/deny these through their permission plugin
(LuckPerms, etc.).

Providers may define their own permission nodes — consult the provider mod's documentation.

## Permission Providers

To integrate with a permission plugin, implement `PermissionProvider` and register it via
`SafeCatAPI.registerAdapter()` or `SafeCatRegistry.register()`:

```java
public class MyPermProvider implements PermissionProvider {
  public String getProviderId() { return "myperms"; }
  public int priority() { return 100; }
  public void init(SafeCatRegistry registry) { registry.register(this); }
  public CompletableFuture<Boolean> hasPermission(UUID player, String perm) {
    return hasPermission(player, perm, null);
  }
  public CompletableFuture<Boolean> hasPermission(UUID player, String perm, String context) {
    // check your permission system
  }
}
```

See the [example-permissions](../../example-permissions/) module for a working example.

## PermissionCheckEvent

Before consulting `PermissionProvider`s, SafeCat fires a `PermissionCheckEvent`. Listeners can
call `setResult(true/false)` to override the entire permission check and short-circuit the
provider chain. This is useful for granting admin bypasses or blocking specific actions
regardless of permission plugin state.
