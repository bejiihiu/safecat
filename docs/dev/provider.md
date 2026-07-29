# Writing a SafeCat Provider

- [Add the Dependency](#add-the-dependency)
- [Overview](#overview)
- [Step 1: Implement CurrencyProvider](#step-1-implement-currencyprovider)
- [Step 2: Register via registerAdapter() (Simpler)](#step-2-register-via-registeradapter-simpler)
- [Step 3: Register via ServiceLoader](#step-3-register-via-serviceloader)
- [Step 4 (Alternative): Register via EventBus](#step-4-alternative-register-via-eventbus)
- [Testing Your Provider](#testing-your-provider)
- [Best Practices](#best-practices)

---

## Add the Dependency

SafeCat's API and common modules are published to `maven.bejiihiu.kz`. Add the repository and
dependency to your `build.gradle`:

```gradle
repositories {
    maven { url "https://maven.bejiihiu.kz/releases" }
}

dependencies {
    implementation "kz.bejiihiu:safecat-api:1.0.0"
    // optional — test helpers, SafeCatCore for testing
    testImplementation "kz.bejiihiu:safecat-common:1.0.0"
}
```

---

## Overview

A SafeCat provider bridges an economy mod (or a custom backend) to the SafeCat ecosystem. You
implement `CurrencyProvider`, register it so SafeCat discovers it, and SafeCat handles the rest.

There are two registration methods:

| Method              | Platforms          | Mechanism                             |
|---------------------|--------------------|---------------------------------------|
| **ServiceLoader**   | All (Forge, Fabric, NeoForge) | `META-INF/services/` file |
| **EventBus**        | Forge, NeoForge only | `@SubscribeEvent` on platform bus |

Both can coexist in the same provider jar.

## Step 1: Implement CurrencyProvider

Create a class that implements `kz.bejiihiu.safecat.api.CurrencyProvider`:

```java
package com.example.myeco;

import kz.bejiihiu.safecat.api.*;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MyEcoProvider implements CurrencyProvider {

  private final MyEcoBackend backend = new MyEcoBackend();

  @Override
  public String getCurrencyId() {
    return "myeco:coin";
  }

  @Override
  public void init(SafeCatRegistry registry) {
    // 1. Register the currency so SafeCat knows its display name, symbol, and type
    registry.register(new Currency(
        getCurrencyId(),
        "MyEco Coin",
        "M",
        CurrencyType.TOKEN
    ));
    // 2. Register this provider so SafeCat routes balance calls to it
    registry.register(this);
  }

  @Override
  public CompletableFuture<BigDecimal> getBalance(UUID player) {
    return CompletableFuture.completedFuture(backend.getBalance(player));
  }

  @Override
  public CompletableFuture<TransactionResult> withdraw(
      UUID player, BigDecimal amount, EventReason reason) {
    if (amount.signum() <= 0) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(getCurrencyId(), "amount must be positive"));
    }
    if (!backend.hasSufficient(player, amount)) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(getCurrencyId(), "insufficient balance"));
    }
    backend.apply(player, amount.negate());
    return CompletableFuture.completedFuture(
        TransactionResult.success(amount, getCurrencyId()));
  }

  @Override
  public CompletableFuture<TransactionResult> deposit(
      UUID player, BigDecimal amount, EventReason reason) {
    if (amount.signum() <= 0) {
      return CompletableFuture.completedFuture(
          TransactionResult.failure(getCurrencyId(), "amount must be positive"));
    }
    backend.apply(player, amount);
    return CompletableFuture.completedFuture(
        TransactionResult.success(amount, getCurrencyId()));
  }
}
```

### CurrencyProvider Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getCurrencyId()` | `String` | Unique identifier for this provider's currency |
| `init(SafeCatRegistry)` | `void` | Called during SafeCat startup. Register your `Currency` and `this` |
| `priority()` | `int` (default 0) | Higher values mean earlier resolution in the provider chain |
| `getBalance(UUID)` | `CompletableFuture<BigDecimal>` | Current balance for a player |
| `withdraw(UUID, BigDecimal, EventReason)` | `CompletableFuture<TransactionResult>` | Remove funds |
| `deposit(UUID, BigDecimal, EventReason)` | `CompletableFuture<TransactionResult>` | Add funds |
| `supports(UUID)` | `boolean` (default true) | Whether this provider handles the given player |

### The `Currency` Record

```java
new Currency(
    "myeco:coin",     // id — must match getCurrencyId()
    "MyEco Coin",     // displayName
    "M",              // symbol — short display character
    CurrencyType.TOKEN // type — FIAT, DIGITAL, TOKEN, or CUSTOM
)
```

### The `Currency` Type

```java
CurrencyType.FIAT     // Traditional currency (USD, EUR)
CurrencyType.DIGITAL  // Crypto-native (Bitcoin)
CurrencyType.TOKEN    // In-game token (guild coins, event tokens)
CurrencyType.CUSTOM   // Mod-defined
```

## Step 2: Register via `registerAdapter()` (Simpler)

If you're writing a mod and just want to bridge it to SafeCat, use the one-line API:

```java
public class MyAdapter implements CurrencyProvider {
  public MyAdapter() {
    SafeCatAPI.getInstance().registerAdapter(this);
  }
  // ...
}
```

`registerAdapter()` inspects the object via `instanceof` and registers it for every implemented
interface (`CurrencyProvider`, `PermissionProvider`, `ChatProvider`, `CommandProvider`).

No `META-INF/services`, no `ServiceLoader`, no events. Just one call.

**When to use this:** you're writing an adapter mod from scratch, targeting one specific economy
mod. The adapter is yours — you control the constructor.

**When to use ServiceLoader instead:** you're the economy mod author and want seamless auto-discovery
for all SafeCat consumers without any manual setup.

## Step 3: Register via ServiceLoader

ServiceLoader is the platform-independent discovery mechanism. It works on Forge, Fabric,
and NeoForge without any loader-specific code.

1. Create the services file at:
   ```
   src/main/resources/META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider
   ```

2. Add one line — the fully qualified class name of your provider:
   ```
   # My SafeCat provider
   com.example.myeco.MyEcoProvider
   ```

3. That's it. SafeCat discovers it automatically during startup and calls `init()`.

**Verify the file is packaged correctly:**
```
jar tf build/libs/MyEcoMod.jar | findstr "META-INF/services"
```
Should print `META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider`.

## Step 4 (Alternative): Register via EventBus

If you're writing a Forge or NeoForge mod and prefer not to use ServiceLoader, listen for
`RegisterCurrenciesEvent` and `RegisterProvidersEvent` on the platform's event bus.

> SafeCat uses **SafeCatEventBus** — a lightweight `Consumer<T>`-based bus. To subscribe to
> SafeCat events directly (bypassing the platform bridge), use
> `SafeCatAPI.getInstance().getEventBus().on(Type.class, event -> ...)`. See the [Events
> Reference](events.md) for examples.

```java
package com.example.myeco;

import kz.bejiihiu.safecat.api.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class MyEcoRegistration {

  @SubscribeEvent
  public static void onRegisterCurrencies(RegisterCurrenciesEvent event) {
    event.register(new Currency(
        "myeco:coin", "MyEco Coin", "M", CurrencyType.TOKEN
    ));
  }

  @SubscribeEvent
  public static void onRegisterProviders(RegisterProvidersEvent event) {
    event.register(new MyEcoProvider());
  }
}
```

**Pros:** No `META-INF/services` file, natural for Forge/NeoForge developers.  
**Cons:** Only works on platforms with a global event bus (Forge, NeoForge). Fabric has no equivalent.

You can also subscribe to `BalanceRequestEvent` directly in the same subscriber class to
bypass the provider chain entirely:

```java
@SubscribeEvent
public static void onBalanceRequest(BalanceRequestEvent event) {
  if (!event.getCurrencyId().equals("myeco:coin")) return;
  event.setBalance(queryMyBackend(event.getPlayer()));
  event.setCancelled(true); // short-circuit provider chain
}
```

## Extensions

Extensions are deployed as JARs dropped into `config/safecat/extensions/`.  
Each extension checks for its target mod at runtime and registers providers automatically.

| Extension | Type | Target Mod | Status |
|-----------|------|------------|--------|
| `extension-luckperms` | Permission | LuckPerms | ✅ Working |

Build an extension:
```bash
./gradlew :extension-luckperms:build
```

For details, see [Integrations](integrations.md).

## Testing Your Provider

SafeCat provides a test framework in the `common` module. Extend it in your project:

```java
import kz.bejiihiu.safecat.common.SafeCatCore;
import java.util.concurrent.ExecutionException;

public class MyEcoProviderTest {
  private SafeCatAPI api;

  @BeforeEach
  void setUp() throws ExecutionException, InterruptedException {
    SafeCatCore.initialize().get(); // starts SafeCat with ServiceLoader discovery
    api = SafeCatAPI.getInstance();
  }

  @Test
  void balanceStartsAtZero() throws Exception {
    BigDecimal balance = api.getBalance(UUID.randomUUID(), "myeco:coin").get();
    assertEquals(BigDecimal.ZERO, balance);
  }

  @Test
  void depositIncreasesBalance() throws Exception {
    UUID player = UUID.randomUUID();
    api.deposit(player, "myeco:coin", BigDecimal.TEN, EventReason.ADMIN).get();
    BigDecimal balance = api.getBalance(player, "myeco:coin").get();
    assertEquals(new BigDecimal("10"), balance);
  }

  @Test
  void withdrawFailsOnInsufficientBalance() throws Exception {
    TransactionResult result = api.withdraw(
        UUID.randomUUID(), "myeco:coin", BigDecimal.ONE, EventReason.SHOP_PURCHASE).get();
    assertFalse(result.success());
  }
}
```

For more realistic integration tests, stand up a Minecraft test environment with your mod
and SafeCat together.

## Best Practices

- **Use namespaced currency IDs.** Prefer `modid:currencyname` (e.g. `ftbmoney:coin`,
  `numismatic:coin`). Avoid generic names like `"money"` or `"coins"` to prevent conflicts.

- **Return `CompletableFuture` correctly.** Never block in provider methods. If your backend
  is synchronous, wrap it with `CompletableFuture.completedFuture()`. If it's async, chain
  properly.

- **Amount validation is now centralized.** `SafeCatAPIImpl` validates
  `amount.signum() <= 0` before any provider is called. Providers no longer need to check for
  negative or zero amounts — but it's harmless to do so as a safety net.

- **Atomic balance operations.** Use `ConcurrentHashMap.compute()` or database transactions
  for balance changes to avoid race conditions.

- **Use `signum()` for zero/negative checks.** `BigDecimal` comparison with `compareTo()` is
  correct but verbose. `signum() <= 0` covers both zero and negative.

- **Keep `init()` lightweight.** Register your currency and provider, but defer heavy
  initialization to a background thread if needed.

- **The `/safecat` command is built-in.** SafeCat registers it automatically during
  `SafeCatCore.initialize()`. It displays version info, currencies, and provider status.
  You don't need to register it yourself.
