# Using SafeCatAPI (Consumer Guide)

- [Add the Dependency](#add-the-dependency)
- [Getting the API Instance](#getting-the-api-instance)
- [Balance Operations](#balance-operations)
- [Event Handling](#event-handling)
- [Commands](#commands)
- [Best Practices](#best-practices)
- [See Also](#see-also)

---

## Add the Dependency

Add the SafeCat repository and API dependency to your `build.gradle`:

```gradle
repositories {
    maven { url "https://maven.bejiihiu.kz/releases" }
}

dependencies {
    implementation "kz.bejiihiu:safecat-api:1.0.0"
}
```

---

## Getting the API Instance

`SafeCatAPI` is a thread-safe singleton. Obtain it via `getInstance()`:

```java
import kz.bejiihiu.safecat.api.SafeCatAPI;

var api = SafeCatAPI.getInstance();
```

**Important:** Call this after SafeCat has initialized — typically in your mod's common setup
phase, not during construction. See [Troubleshooting: API not initialized](../admin/troubleshooting.md#api-not-initialized).

## Balance Operations

### Lookup Currencies

```java
// Get all registered currencies
Set<Currency> all = api.getCurrencies();

// Find a specific currency
Currency currency = api.getCurrency("numismatic:coin");
// currency.id() → "numismatic:coin"
// currency.displayName() → "Numismatic Coin"
// currency.symbol() → "Ⓝ"
// currency.type() → CurrencyType.TOKEN
```

### Get Balance

```java
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

CompletableFuture<BigDecimal> future = api.getBalance(playerUUID, "numismatic:coin");
BigDecimal balance = future.join(); // blocks current thread — use with care
```

The balance lookup fires a `BalanceRequestEvent` first. If a listener cancels it and sets a
value, that value is returned without consulting any provider.

### Withdraw

```java
CompletableFuture<TransactionResult> future = api.withdraw(
    playerUUID,
    "numismatic:coin",
    new BigDecimal("10.00"),
    EventReason.SHOP_PURCHASE
);

TransactionResult result = future.join();
if (result.success()) {
  // item was purchased
} else {
  // insufficient balance or other failure
  player.sendMessage(result.message()); // "insufficient balance"
}
```

Withdraw fires `BalanceChangeEvent` before the provider call. If a listener cancels it, the
transaction fails with `"cancelled by event handler"`.

### Deposit

```java
CompletableFuture<TransactionResult> future = api.deposit(
    playerUUID,
    "numismatic:coin",
    new BigDecimal("10.00"),
    EventReason.QUEST_REWARD
);
```

Deposit also fires `BalanceChangeEvent` before the provider call.

### TransactionResult

```java
TransactionResult result = ...;
result.success();   // boolean
result.amount();    // BigDecimal — net amount
result.currencyId(); // String
result.message();   // String — human-readable outcome
result.timestamp(); // Instant — when the result was created
```

Create results in your own code:

```java
TransactionResult.success(amount, currencyId);
TransactionResult.failure(currencyId, "reason");
```

### EventReason

```java
EventReason.SHOP_PURCHASE  // Shop or marketplace
EventReason.QUEST_REWARD   // Quest completion
EventReason.ADMIN          // Manual admin adjustment
EventReason.TAX            // Tax or fee
EventReason.INTEREST       // Interest accrual
EventReason.TRANSFER       // Player-to-player transfer
```

## Event Handling

SafeCat fires events through two parallel systems (see [Events Reference](events.md)):

1. **SafeCatEventBus** (api module) — platform-independent, type-safe, zero dependencies
2. **Platform event bus** (Forge/NeoForge) — `@SubscribeEvent` style via BridgeEvent wrappers

### Subscribing via SafeCatEventBus

SafeCat ships its own `SafeCatEventBus` — a lightweight, type-safe bus using `Consumer<T>`
handlers. No annotations, no reflection, no external dependencies:

```java
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.SafeCatAPI;

var bus = SafeCatAPI.getInstance().getEventBus();
bus.on(BalanceChangeEvent.class, event -> {
  if (isPvpZone(event.getPlayer())) {
    event.setCancelled(true);
  }
});
```

The EventBus is thread-safe (`ConcurrentHashMap` + `CopyOnWriteArrayList`) and lives entirely
in the `api` module — zero dependencies on any platform.

### Subscribing via Forge Event Bus

SafeCat forwards events to the Forge/NeoForge event bus wrapped in a typed bridge class:

```java
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.Safecat.ForgeBridgeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MyHandler {
  @SubscribeEvent
  public void onSafecatEvent(ForgeBridgeEvent bridge) {
    if (bridge.unwrap() instanceof BalanceChangeEvent bce) {
      handleBalanceChange(bce);
    }
  }
}
```

**Prefer subscribing via SafeCatEventBus** — it works on all platforms without a Forge
dependency. See above for the recommended approach.

### Available Events

| Event | Cancelable | When | Fields |
|-------|-----------|------|--------|
| `BalanceChangeEvent` | Yes | Before withdraw/deposit | player, currencyId, amount, reason, operation |
| `BalanceRequestEvent` | Yes | Before balance lookup | player, currencyId, balance (settable) |
| `TransactionEvent` | No | After successful transaction | from, to, currencyId, amount, reason, result |
| `RegisterCurrenciesEvent` | No | During startup | registry |
| `RegisterProvidersEvent` | No | During startup | registry |

Full details in [Events Reference](events.md).

## Commands

SafeCat provides a platform-independent command system. Register commands via the API:

```java
api.registerCommand(new SimpleCommand(
    "mycommand",
    "Does something cool",
    "myperm.mymod.mycommand",
    (sender, args) -> {
      sender.sendMessage("Hello, " + sender.getName());
      return CompletableFuture.completedFuture(true);
    }
));
```

Or implement `CommandProvider` directly:

```java
public class MyCommand implements CommandProvider {
  @Override public String getName() { return "mycommand"; }
  @Override public String getDescription() { return "Does something cool"; }
  @Override public String getPermission() { return "myperm.mymod.mycommand"; }

  @Override
  public CompletableFuture<Boolean> execute(CommandSender sender, String[] args) {
    sender.sendMessage("Hello, " + sender.getName());
    return CompletableFuture.completedFuture(true);
  }
}
```

### Built-in `/safecat` Command

SafeCat registers a built-in `/safecat` command automatically during startup. It requires no
provider, has no permission requirement, and works on all platforms:

| Subcommand | Description |
|------------|-------------|
| `/safecat help` | Show help message and version |
| `/safecat status` | Show loaded providers and currencies |
| `/safecat currencies` | List all registered currencies |

The command is registered by `SafecatCommand` in the `common` module during
`SafeCatCore.initialize()`.

## Best Practices

1. **Always use `SafeCatAPI.getInstance()`.** Never cache the instance in a static field —
   SafeCat may need to reinitialize it during a reload.

2. **Handle failures gracefully.** `withdraw()` and `deposit()` return a result even on
   failure. Check `result.success()` before proceeding — don't assume the transaction worked.
   Amount validation (positive amounts only) is handled centrally — you'll never get a
   negative-amount error from a provider.

3. **Don't block the server thread.** `CompletableFuture.join()` blocks the current thread.
   In command handlers and event listeners, use `.thenAccept()` or `.thenCompose()` instead.

4. **Use `BigDecimal` correctly.** Amounts are `BigDecimal`. Never use `double` or `float`
   for monetary values. Create instances with `BigDecimal.valueOf()` or `new BigDecimal(String)`.

5. **Check currency existence.** `api.getCurrency(id)` returns `null` if the currency isn't
   registered. Always check for null before using the result.

6. **Listen to `TransactionEvent` for audit logs.** It's fired after every successful
   transaction with full context: who sent, who received, how much, and why.

## See Also

- [Events Reference](events.md) — complete event catalog
- [Writing a SafeCat Provider](provider.md) — if you need to create a provider
- [Architecture](architecture.md) — module structure and design rationale
- [Migration V1 to V2](migration-v1-to-v2.md) — compatibility guarantees
