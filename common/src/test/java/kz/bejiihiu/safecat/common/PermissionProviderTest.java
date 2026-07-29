package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.PermissionCheckEvent;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.SafeCatEventBus;
import kz.bejiihiu.safecat.api.SafeCatRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PermissionProviderTest {

  private static final UUID PLAYER = UUID.randomUUID();
  private static final String PERMISSION = "group.vip";

  private SafeCatRegistryImpl registry;
  private SafeCatEventBus eventBus;
  private SafeCatAPIImpl api;

  @BeforeEach
  void setUp() {
    eventBus = spy(new SafeCatEventBus());
    registry = new SafeCatRegistryImpl();
    api = new SafeCatAPIImpl(registry, eventBus);
  }

  @Test
  void noProviders_returnsFalse() {
    assertFalse(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void providerReturnsTrue() {
    registry.register(provider("test", 0, true));
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void providerReturnsFalse() {
    registry.register(provider("test", 0, false));
    assertFalse(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void highestPriorityWins() {
    PermissionProvider low = provider("low", 0, false);
    PermissionProvider high = provider("high", 10, true);
    registry.register(low);
    registry.register(high);
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void highPriorityReturnsFalse_fallsBackToLower() {
    PermissionProvider high = provider("high", 10, false);
    PermissionProvider low = provider("low", 0, true);
    registry.register(high);
    registry.register(low);
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void allProvidersFalse_returnsFalse() {
    registry.register(provider("a", 10, false));
    registry.register(provider("b", 0, false));
    assertFalse(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void permissionCheckEventCancelledWithTrue_overridesProviders() {
    eventBus.on(
        PermissionCheckEvent.class,
        e -> {
          e.setResult(true);
          e.setCancelled(true);
        });
    registry.register(provider("test", 0, false));
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void permissionCheckEventCancelledWithFalse_overridesProviders() {
    eventBus.on(
        PermissionCheckEvent.class,
        e -> {
          e.setResult(false);
          e.setCancelled(true);
        });
    registry.register(provider("test", 0, true));
    assertFalse(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void permissionCheckEventCancelledWithoutResult_fallsThrough() {
    eventBus.on(PermissionCheckEvent.class, e -> e.setCancelled(true));
    registry.register(provider("test", 0, true));
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void hasPermissionWithContext() {
    PermissionProvider ctxProvider =
        new PermissionProvider() {
          @Override
          public String getProviderId() {
            return "ctx";
          }

          @Override
          public void init(SafeCatRegistry reg) {
            reg.register(this);
          }

          @Override
          public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
            return CompletableFuture.completedFuture(false);
          }

          @Override
          public CompletableFuture<Boolean> hasPermission(
              UUID player, String permission, String context) {
            return CompletableFuture.completedFuture("world:nether".equals(context));
          }
        };
    registry.register(ctxProvider);
    assertTrue(api.hasPermission(PLAYER, PERMISSION, "world:nether").join());
    assertFalse(api.hasPermission(PLAYER, PERMISSION, "world:overworld").join());
  }

  @Test
  void getPermissionProviderById() {
    PermissionProvider p = provider("my-provider", 0, true);
    registry.register(p);
    assertSame(p, registry.getPermissionProvider("my-provider"));
  }

  @Test
  void getPermissionProvidersReturnsAll() {
    PermissionProvider a = provider("a", 0, true);
    PermissionProvider b = provider("b", 0, false);
    registry.register(a);
    registry.register(b);
    assertEquals(2, registry.getPermissionProviders().size());
  }

  @Test
  void priorityOrderInChain() {
    PermissionProvider normal = provider("normal", 0, false);
    PermissionProvider earliest = provider("earliest", 100, true);
    registry.register(normal);
    registry.register(earliest);
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  @Test
  void providerThrows_fallsThrough() {
    PermissionProvider broken =
        new PermissionProvider() {
          @Override
          public String getProviderId() {
            return "broken";
          }

          @Override
          public void init(SafeCatRegistry reg) {
            reg.register(this);
          }

          @Override
          public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
            return CompletableFuture.failedFuture(new RuntimeException("boom"));
          }

          @Override
          public CompletableFuture<Boolean> hasPermission(
              UUID player, String permission, String context) {
            return CompletableFuture.failedFuture(new RuntimeException("boom"));
          }
        };
    PermissionProvider working = provider("working", 0, true);
    registry.register(broken);
    registry.register(working);
    assertTrue(api.hasPermission(PLAYER, PERMISSION).join());
  }

  private static PermissionProvider provider(String id, int priority, boolean result) {
    return new PermissionProvider() {
      @Override
      public String getProviderId() {
        return id;
      }

      @Override
      public int priority() {
        return priority;
      }

      @Override
      public void init(SafeCatRegistry reg) {
        reg.register(this);
      }

      @Override
      public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
        return CompletableFuture.completedFuture(result);
      }

      @Override
      public CompletableFuture<Boolean> hasPermission(
          UUID player, String permission, String context) {
        return CompletableFuture.completedFuture(result);
      }
    };
  }
}
