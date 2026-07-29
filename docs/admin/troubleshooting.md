# Troubleshooting FAQ

- [ServiceLoader finds no providers](#serviceloader-finds-no-providers)
- [My economy mod isn't visible](#my-economy-mod-isnt-visible)
- [Currency conflict](#currency-conflict)
- [Transactions failing](#transactions-failing)
- [Multi-loader issues](#multi-loader-issues)
- [API not initialized](#api-not-initialized)
- [Init events not firing](#init-events-not-firing)

---

## ServiceLoader finds no providers

**Symptom:** SafeCat starts but logs `WARNING: No CurrencyProvider implementations found`. The
server runs normally but no currencies are registered.

**Cause:** ServiceLoader cannot find any `META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider`
file on the classpath, or the file references a class that isn't loadable.

**Checklist:**

1. **Is the services file packaged?** Inspect your provider jar:
   ```
   jar tf provider.jar | findstr "META-INF/services"
   ```
   Expected: `META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider`

2. **Is the class name correct?** The file must contain exactly one line — the fully qualified
   name of your `CurrencyProvider` implementation (e.g. `com.example.myeco.MyEcoProvider`).
   No trailing whitespace, no extra lines.

3. **Is the implementation class on the module path?** On Java 9+, ServiceLoader respects
   module boundaries. If your jar has a `module-info.class`, make sure it either:
   - `provides kz.bejiihiu.safecat.api.CurrencyProvider with com.example.myeco.MyEcoProvider;`
   - Or remove the `module-info.class` and rely on `META-INF/services` alone.

4. **Does the class implement `CurrencyProvider`?** The listed class must directly implement
   `kz.bejiihiu.safecat.api.CurrencyProvider`. Implementing a sub-interface does not count.

5. **Is there a class loader issue?** On Forge/NeoForge, ServiceLoader only scans the mod's
   own jar, not the global classpath. If your implementation is in a different jar than the
   services file, it won't be discovered.

**Fix:** Add the services file, verify the class name, and rebuild.

---

## My economy mod isn't visible

**Symptom:** SafeCat starts without errors, but no currencies or providers are registered.
The server log shows `WARNING: No CurrencyProvider implementations found`.

**Possible causes:**

1. **Economy mod doesn't implement `CurrencyProvider`.** Only mods that implement the SafeCat
   `CurrencyProvider` interface are discovered. If your economy mod wasn't written for SafeCat,
   it won't work. See [Provider Guide](../dev/provider.md) for how to write an integration.

2. **Missing or wrong `META-INF/services` file.** ServiceLoader requires an exact match:
   `META-INF/services/kz.bejiihiu.safecat.api.CurrencyProvider` with the fully qualified
   class name of your implementation. Check that the file exists in the jar:
   ```
   jar tf MyEconomyMod.jar | findstr "META-INF/services"
   ```
   The file must contain exactly one line: the fully qualified class name of your provider.

3. **Class loader isolation.** On Forge/NeoForge, mods in `mods/` use isolated class loaders.
   ServiceLoader scans the mod's own jar, not the global classpath. Ensure your provider is in
   the same jar as the ServiceLoader file.

4. **EventBus registration never fired.** If your provider relies on `@SubscribeEvent` for
   `RegisterCurrenciesEvent`/`RegisterProvidersEvent` (Forge approach), check that your
   subscriber class is annotated with `@Mod.EventBusSubscriber` and the mod is loaded.

5. **Run `/safecat status`** in-game or from console. It shows how many currencies are
   registered and confirms whether the config loaded.

## Currency conflict

**Symptom:** Two economy mods register a currency with the same `currencyId` (e.g. both use
`"money"`). The second registration silently overwrites the first. Undefined behavior follows.

**Solution:**

Use namespaced currency IDs. SafeCat recommends `modid:currencyname` format:
- `ftbmoney:coin`
- `numismatic:coin`
- `safecat:default`

Currency IDs are strings — no central registry enforces uniqueness. Coordinate with other mod
authors or use a configurable ID in your provider.

If you cannot change the currency ID, set provider priority (`CurrencyProvider.priority()`)
to control which provider wins when duplicates are registered:

```java
@Override
public int priority() {
  return 100; // higher = resolved first
}
```

Currency registration uses `ConcurrentHashMap.put()` — later registrations overwrite earlier
ones. Priority does not affect currency registration ordering; it only affects provider chain
resolution during balance/transaction calls.

## Transactions failing

**Symptom:** `SafeCatAPI.withdraw()` or `deposit()` returns `TransactionResult(success=false)`
with a message like "insufficient balance", "no provider for currency: ...", or "cancelled by
event handler".

**Checklist:**

1. **Is the currency registered?** Call `SafeCatAPI.getInstance().getCurrency(id)` — if it
   returns null, no provider registered that currency. Check the server log for provider
   registration messages.

2. **Does the provider handle this player?** `CurrencyProvider.supports(UUID)` defaults to
   `true` but can be overridden for permission-based gating or dimension-based economies.
   The provider chain skips non-supporting providers.

3. **Is a listener cancelling the transaction?** `BalanceChangeEvent` is cancelable.
   Any event handler (including mods you installed) can call `setCancelled(true)` to prevent
   the transaction. Search your server log for "cancelled by event handler".

4. **Is the amount valid?** Negative and zero amounts are rejected by providers. Use positive
   `BigDecimal` values.

5. **Balances use BigDecimal.** Floating-point errors accumulate. Always use
   `BigDecimal.valueOf()` or `new BigDecimal(String)` — never `new BigDecimal(double)`.

## Multi-loader issues

**Symptom:** SafeCat works on Forge but not Fabric, or vice versa.

**Check that you downloaded the correct jar.** Each loader has its own artifact:
- `safecat-forge-*.jar` — for MinecraftForge
- `safecat-fabric-*.jar` — for Fabric Loader
- `safecat-neoforge-*.jar` — for NeoForge

The jars are NOT interchangeable. Putting a Forge jar in a Fabric server's `mods/` folder
will not load.

**EventBus registration (Forge/NeoForge only):** If your provider uses the EventBus approach
(`@SubscribeEvent` for `RegisterCurrenciesEvent`), it only works on Forge and NeoForge.
Fabric providers must use ServiceLoader (`META-INF/services`). See [Provider Guide](../dev/provider.md)
for both approaches.

## API not initialized

**Symptom:** `SafeCatAPI.getInstance()` throws `IllegalStateException` with message
"SafeCatAPI has not been initialised yet".

**Cause:** Something called `getInstance()` before SafeCatCore.initialize() completed.
This typically happens during mod construction time — SafeCat initializes during a loader's
common setup phase, not during class loading.

**Fix:** Call `getInstance()` after your mod's setup phase, not in the constructor. Forge
example:

```java
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MyMod {
  @SubscribeEvent
  public static void onCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      var api = SafeCatAPI.getInstance(); // safe here
    });
  }
}
```

## Init events not firing

**Symptom:** `RegisterCurrenciesEvent`, `RegisterProvidersEvent`, or `CommandRegistrationEvent`
listeners never fire, even though the mod is loaded.

**Cause:** These events are posted during `SafeCatRegistryImpl.initialize()`, which runs
before platform event bus listeners have subscribed. The init events are fired inside
`SafeCatCore.initialize()` — at that point the SafeCat-internal event bus exists, but platform
mods that subscribe to SafeCat events haven't registered their handlers yet.

**Fix:** Platform-specific initialization code should re-fire these events after subscribing
its listeners. For custom providers, use `ServiceLoader` discovery or the `SafeCatAPI.registerAdapter()`
approach instead of relying on init events:

```java
public class MyProvider extends BaseCurrencyAdapter {
  public MyProvider() {
    // registerAdapter works regardless of event timing
    SafeCatAPI.getInstance().registerAdapter(this);
  }
}
```
