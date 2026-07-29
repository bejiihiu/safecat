package kz.bejiihiu.safecat.extension.example;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.ChatProvider;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyProvider;
import kz.bejiihiu.safecat.api.CurrencyType;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.PermissionProvider;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import kz.bejiihiu.safecat.api.SafeCatRegistry;
import kz.bejiihiu.safecat.api.TransactionResult;
import kz.bejiihiu.safecat.extension.SafeCatExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example extension — copy this module to create your own integration.
 *
 * <p>An extension is a JAR dropped into {@code config/safecat/extensions/}. SafeCat loads it at
 * startup and calls {@link #init()}. From there you can register any combination of providers.
 *
 * <p>This example registers all three provider types with stub implementations. Replace the stubs
 * with real API calls to your target mod.
 */
public class ExampleExtension implements SafeCatExtension {

  private static final Logger LOG = LoggerFactory.getLogger(ExampleExtension.class);

  @Override
  public String id() {
    return "example";
  }

  @Override
  public String name() {
    return "Example Extension";
  }

  @Override
  public void init() {
    // Check if your target mod is loaded:
    //   Fabric: FabricLoader.getInstance().isModLoaded("modid")
    //   Class: try { Class.forName("some.ModApi"); } catch (ClassNotFoundException e) { return; }
    // If not loaded — just return, no-op is fine.
    // If loaded — register providers:

    SafeCatAPI.getInstance().registerAdapter(new ExampleCurrencyProvider());
    SafeCatAPI.getInstance().registerAdapter(new ExamplePermissionProvider());
    SafeCatAPI.getInstance().registerAdapter(new ExampleChatProvider());

    LOG.info("Example extension loaded");
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CurrencyProvider — implement if your target mod has an economy.
  // ═══════════════════════════════════════════════════════════════════════════

  private static class ExampleCurrencyProvider implements CurrencyProvider {

    @Override
    public String getCurrencyId() {
      return "example:coin";
    }

    @Override
    public void init(SafeCatRegistry registry) {
      registry.register(new Currency(getCurrencyId(), "Example Coin", "E$", CurrencyType.TOKEN));
      registry.register(this);
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID player) {
      // return MyModAPI.getBalance(player);
      return CompletableFuture.completedFuture(BigDecimal.ZERO);
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(
        UUID player, BigDecimal amount, EventReason reason) {
      // boolean ok = MyModAPI.withdraw(player, amount);
      // return ok ? ok(amount) : fail("insufficient balance");
      return ok(amount);
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(
        UUID player, BigDecimal amount, EventReason reason) {
      // MyModAPI.deposit(player, amount);
      return ok(amount);
    }

    private CompletableFuture<TransactionResult> ok(BigDecimal amount) {
      return CompletableFuture.completedFuture(TransactionResult.success(amount, getCurrencyId()));
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PermissionProvider — implement if your target mod manages permissions.
  // ═══════════════════════════════════════════════════════════════════════════

  private static class ExamplePermissionProvider implements PermissionProvider {

    @Override
    public String getProviderId() {
      return "example-perms";
    }

    @Override
    public void init(SafeCatRegistry registry) {
      registry.register(this);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(UUID player, String permission) {
      return hasPermission(player, permission, null);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(
        UUID player, String permission, String context) {
      // return CompletableFuture.completedFuture(MyPermsAPI.has(player, permission));
      return CompletableFuture.completedFuture(false);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ChatProvider — implement if your target mod provides player chat data.
  // ═══════════════════════════════════════════════════════════════════════════

  private static class ExampleChatProvider implements ChatProvider {

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
      // return CompletableFuture.completedFuture(MyChatAPI.getPrefix(player));
      return CompletableFuture.completedFuture(Optional.empty());
    }
  }
}
