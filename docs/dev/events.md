# Events in SafeCat

- [Architecture Overview](#architecture-overview)
- [Event Base Class](#event-base-class)
- [SafeCatEventBus API](#safecateventbus-api)
- [Event Catalog](#event-catalog)
  - [BalanceChangeEvent](#balancechangeevent)
  - [BalanceRequestEvent](#balancerequestevent)
  - [TransactionEvent](#transactionevent)
  - [RegisterCurrenciesEvent](#registercurrenciesevent)
  - [RegisterProvidersEvent](#registerprovidersevent)
  - [CommandRegistrationEvent](#commandregistrationevent)
- [Subscription Examples](#subscription-examples)
- [Thread Safety](#thread-safety)

---

## Architecture Overview

SafeCat uses two parallel event systems:

```
┌──────────────────────────────────────────────────┐
│  SafeCatEventBus (api module, no deps)            │
│  ───────────────────────────────────────────────  │
│  Subscribe: bus.on(Type.class, event -> { ... }) │
│  Post:     eventBus.post(new FooEvent(...))       │
│  Thread-safe, zero deps on Minecraft              │
└───────────┬──────────────────────────────────────┘
            │  forwarded by platform adapter
            ▼
┌──────────────────────────────────────────────────┐
│  Platform Event Bus (MinecraftForge / NeoForge)  │
│  ───────────────────────────────────────────────  │
│  Subscribe: @SubscribeEvent on BridgeEvent class │
│  Post:     NeoForge.EVENT_BUS.post(event)         │
│  Only available on Forge/NeoForge                 │
└──────────────────────────────────────────────────┘
```

Platform adapters subscribe to SafeCatEventBus via `bus.on()` and forward every event to the
platform event bus wrapped in a typed BridgeEvent. This means a consumer can subscribe via
either system.

## Event Base Class

```java
public abstract class Event {
  protected Event() {}
  private boolean cancelled;

  public boolean isCancelled() { return cancelled; }
  public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
```

Every SafeCat event extends `Event` and inherits the `cancelled` flag. Not all events
respect cancellation — see each event's description.

## SafeCatEventBus API

`SafeCatEventBus` is the internal event bus — a lightweight alternative to Guava's EventBus
with no annotations, no reflection, and no external dependencies:

```java
public final class SafeCatEventBus {
  public <T> void on(Class<T> type, Consumer<T> handler);
  public <T> void post(T event);
}
```

- **Subscribe:** `bus.on(EventType.class, event -> { ... })`
- **Post:** `bus.post(new MyEvent(...))`

The bus uses `ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>>` internally —
thread-safe for both subscription and dispatch, zero synchronisation overhead.

Obtain the bus from the API:

```java
SafeCatAPI.getInstance().getEventBus().on(BalanceChangeEvent.class, event -> {
  // handle event
});
```

## Event Catalog

### BalanceChangeEvent

Fired **before** a player's balance is modified by `withdraw()` or `deposit()`.
**Cancelable.** Setting `setCancelled(true)` prevents the change — the API returns a
`TransactionResult` with `success=false` and message `"cancelled by event handler"`.

Posted in `SafeCatAPIImpl.withdraw()` / `deposit()`.

```java
new BalanceChangeEvent(player, currencyId, amount, reason, operation)
```

| Field     | Type                          | Description                                |
|-----------|-------------------------------|--------------------------------------------|
| player    | `UUID`                        | The affected player                        |
| currencyId| `String`                      | Which currency is changing                 |
| amount    | `BigDecimal`                  | How much (positive for deposit, negative for withdraw — absolute value) |
| reason    | `EventReason`                 | Why the change was triggered               |
| operation | `Operation` (`WITHDRAW`/`DEPOSIT`) | Direction of the change              |

**Cancelable:** Yes. Cancel to prevent the balance modification.

### BalanceRequestEvent

Fired **before** a balance lookup via `getBalance()`. **Cancelable.** When cancelled
**and** `getBalance()` returns a non-null value, that value is returned without consulting
any provider. If cancelled but balance is null, the provider chain is still consulted.

Posted in `SafeCatAPIImpl.getBalance()`.

```java
new BalanceRequestEvent(currencyId, player)
```

| Field     | Type         | Description                          |
|-----------|--------------|--------------------------------------|
| currencyId| `String`     | The currency to look up              |
| player    | `UUID`       | The player to look up                |

**Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `getBalance()` | `BigDecimal` | Current result balance (may be null) |
| `setBalance(BigDecimal)` | `void` | Override the balance value |

**Cancelable:** Yes. If cancelled and balance is set, short-circuits provider resolution.

### TransactionEvent

Fired **after** a successful deposit or withdrawal. **Informational only** — the
transaction has already been applied. Not cancelable.

Posted in `SafeCatAPIImpl` after a successful provider call.

```java
new TransactionEvent(from, to, currencyId, amount, reason, result)
```

| Field     | Type               | Description                                      |
|-----------|--------------------|--------------------------------------------------|
| from      | `UUID`             | Sender (zero-UUID `00000000-0000-0000-0000-000000000000` for system-generated) |
| to        | `UUID`             | Receiver                                         |
| currencyId| `String`           | Which currency                                   |
| amount    | `BigDecimal`       | Amount transferred (positive)                    |
| reason    | `EventReason`      | Why the transfer happened                        |
| result    | `TransactionResult`| The full result object (success, amount, message, timestamp) |

**Cancelable:** No.

### RegisterCurrenciesEvent

Fired **during initialisation** after ServiceLoader providers have been loaded.
Providers register their `Currency` types in response. Non-cancelable.

Posted in `SafeCatRegistryImpl.initialize()`.

```java
new RegisterCurrenciesEvent(registry)
```

| Field    | Type             | Description                       |
|----------|------------------|-----------------------------------|
| registry | `SafeCatRegistry`| Registry to register into         |

**Convenience method:** `register(Currency currency)` — delegates to `registry.register(currency)`.

**Cancelable:** No.

### RegisterProvidersEvent

Fired **during initialisation** after ServiceLoader providers have been loaded.
Providers register their `CurrencyProvider` instances in response. Non-cancelable.

Posted in `SafeCatRegistryImpl.initialize()`.

```java
new RegisterProvidersEvent(registry)
```

| Field    | Type             | Description                       |
|----------|------------------|-----------------------------------|
| registry | `SafeCatRegistry`| Registry to register into         |

**Convenience method:** `register(CurrencyProvider provider)` — delegates to `registry.register(provider)`.

**Cancelable:** No.

### CommandRegistrationEvent

Fired **during initialisation** after providers and currencies are registered.
Commands register themselves in response. Non-cancelable.

Posted in `SafeCatRegistryImpl.initialize()`.

```java
new CommandRegistrationEvent(registry)
```

| Field    | Type             | Description                       |
|----------|------------------|-----------------------------------|
| registry | `SafeCatRegistry`| Registry to register into         |

**Cancelable:** No.

---

## Event Summary

| Event                     | Cancelable | When Fired                                                |
|---------------------------|------------|-----------------------------------------------------------|
| `BalanceChangeEvent`      | Yes        | Before `withdraw()` / `deposit()` — prevents the change   |
| `BalanceRequestEvent`     | Yes        | Before `getBalance()` — short-circuits provider chain     |
| `TransactionEvent`        | No         | After successful transaction — audit/logging only         |
| `RegisterCurrenciesEvent` | No         | During startup — register your `Currency` types           |
| `RegisterProvidersEvent`  | No         | During startup — register your `CurrencyProvider`         |
| `CommandRegistrationEvent`| No         | During startup — register your commands                   |

## Subscription Examples

### Via SafeCatEventBus

SafeCat's own `SafeCatEventBus` uses `Consumer<T>` handlers — no annotations, no reflection:

```java
import kz.bejiihiu.safecat.api.BalanceChangeEvent;
import kz.bejiihiu.safecat.api.SafeCatAPI;

SafeCatAPI.getInstance().getEventBus().on(BalanceChangeEvent.class, event -> {
  if (isHoldingItem(event.getPlayer(), "ban_hammer")) {
    event.setCancelled(true);
  }
});
```

### Via Forge Event Bus

```java
import net.minecraftforge.eventbus.api.SubscribeEvent;
import kz.bejiihiu.safecat.Safecat.ForgeBridgeEvent;

public class MyListener {
  @SubscribeEvent
  public void onSafecatEvent(ForgeBridgeEvent bridge) {
    if (bridge.unwrap() instanceof BalanceChangeEvent bce) {
      if (isPvpZone(bce.getPlayer())) {
        bce.setCancelled(true);
      }
    }
  }
}
```

### Via NeoForge Event Bus

Same pattern — subscribe on `Safecat.NeoForgeBridgeEvent`:

### Pattern Matching (Java 21)

Since BridgeEvent wraps `api.Event` directly, you can use Java 21's pattern matching in
`switch` to handle multiple event types in a single subscriber:

```java
@SubscribeEvent
public void onSafecatEvent(ForgeBridgeEvent bridge) {
  switch (bridge.unwrap()) {
    case BalanceChangeEvent bce -> handleBalanceChange(bce);
    case TransactionEvent te    -> auditLog(te);
    case BalanceRequestEvent bre -> cacheLookup(bre);
    default -> {}
  }
}
```

### Registering a Provider via Events (Forge)

```java
@Mod.EventBusSubscriber
public class MyRegistration {
  @SubscribeEvent
  public static void onRegisterCurrencies(RegisterCurrenciesEvent event) {
    event.register(new Currency("myeco:coin", "My Coin", "M", CurrencyType.TOKEN));
  }

  @SubscribeEvent
  public static void onRegisterProviders(RegisterProvidersEvent event) {
    event.register(new MyProvider());
  }
}
```

## Thread Safety

All events are posted on the calling thread. `SafeCatEventBus` is thread-safe by design:
`ConcurrentHashMap` for handler storage and `CopyOnWriteArrayList` for iteration, which
means concurrent subscriptions and dispatches never race. Platform event buses
(MinecraftForge, NeoForge) are not always thread-safe — they should only be posted to
from the main server thread.
