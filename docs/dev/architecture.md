# SafeCat Architecture

- [Rationale: 6 Vault Problems SafeCat Solves](#rationale-6-vault-problems-safecat-solves)
- [Architectural Rules](#architectural-rules)
- [Module Structure](#module-structure)
- [Module Dependency Graph](#module-dependency-graph)
- [Vault Comparison Rationale](#vault-comparison-rationale)

---

## Rationale: 6 Vault Problems SafeCat Solves

SafeCat exists because Vault — despite being the de-facto standard for Bukkit economy — has
fundamental design flaws that cannot be fixed incrementally. Every architectural rule in SafeCat is
a direct response to one of these problems.

### 1. Reflection and instanceof hacks

Vault discovers economy plugins by scanning for known classes via reflection and `instanceof`
checks. This means every new economy plugin requires Vault to know about it in advance, and
plugins that change their internal structure break Vault silently.

**SafeCat's answer:** `java.util.ServiceLoader` and interfaces only. A provider is discovered by
implementing `CurrencyProvider` and registering via `META-INF/services` — or by listening to
`RegisterCurrenciesEvent` / `RegisterProvidersEvent` on the platform event bus (Forge/NeoForge
`@SubscribeEvent`). No reflection, no instanceof — see
[Rule 1](#rule-1-no-reflection-no-instanceof).

### 2. Coupling to concrete implementations

Vault's internal dispatch has branches for specific economy plugins (`if (economy instanceof
CraftConomy)`). This violates the mediator pattern — the API shouldn't know what it's mediating.

**SafeCat's answer:** SafeCat is a pure dispatcher. It routes calls by provider priority
(int value, higher = first) and forwards to whoever registered. It never inspects the provider's
type — see [Rule 2](#rule-2-pure-mediator).

### 3. Single-currency lock-in

Vault assumes one economy, one currency. Servers with multi-currency setups (tokens, coins,
premium currency) have to either chain multiple economy plugins or hack around the API.

**SafeCat's answer:** `Currency` is a first-class citizen with type (FIAT, DIGITAL, TOKEN, CUSTOM),
display name, and symbol. Multiple `CurrencyProvider` instances coexist, each handling their own
currencies.

### 4. Bukkit-only

Vault depends on Bukkit APIs, locking it to a single server platform. Forge, Fabric, and NeoForge
communities have no equivalent.

**SafeCat's answer:** The `api` module has zero dependencies on any loader. Only the adapter modules
(`forge/`, `fabric/`, `neoforge/`) touch loader APIs, and they're governed by the
[3 major versions rule](#rule-3-3-major-versions-rule-for-loader-api).

### 5. No async, no thread safety

Vault operations run synchronously on the server thread. Any blocking call stalls the entire server.

**SafeCat's answer:** Every `CurrencyProvider` method returns `CompletableFuture<>`. Core
dispatchers are thread-safe with `ConcurrentHashMap` and atomic operations.

### 6. Breaking changes with no migration path

Vault has historically broken its API without notice, deprecation, or bridge artifacts. Mods either
update immediately or break.

**SafeCat's answer:** Strict deprecation policy ([Rule 5](#rule-5-deprecate-immediately)) and
cross-version stability guarantee ([Rule 4](#rule-4-cross-version-stability)). API versions get
`@Deprecated` the day a new version ships, with bridge artifacts keeping old versions alive for
3 major releases.

---

## Architectural Rules

### Rule 1: No Reflection, No instanceof

**SafeCat never reflects on or checks the type of a specific economy mod.**

Provider discovery uses `java.util.ServiceLoader` (META-INF/services) or the user-facing {@link
SafeCatAPI#registerAdapter(Object)} API. Both paths end up in the same registry — the core treats
every `CurrencyProvider` as an opaque interface — it never casts, never checks `instanceof`, never
reflects for provider-specific fields.

What this means for contributors:

- Adding support for a new economy must never require modifying SafeCat's core or API.
- If you need provider-specific behavior, add a method to the `CurrencyProvider` interface
  (with a default implementation), don't inspect the provider at runtime.
- `@SuppressWarnings("unchecked")` and `Field.setAccessible()` are forbidden in core and api
  modules. Platform adapters may use them only for loader-internal mechanics (mixin setup etc.).

### Rule 2: Pure Mediator

**SafeCat dispatches by priority and never knows which mod is behind a call.**

The core implements a pure mediator:

1. A call to `SafeCatAPI.withdraw()` enters the provider chain, sorted by priority descending.
2. Each provider in the chain gets a chance to handle the call.
3. If a provider returns `unsupported()` (or fails), the next provider in priority is tried.
4. SafeCat has no concept of "primary provider" or "default provider" — the chain is the model.

This means:

- Providers compete on priority, not on name or registration order.
- Removing or adding a provider changes behavior implicitly through the chain.
- SafeCat cannot be asked "which mod handles currency X?" — by design. It delegates and moves on.

### Rule 3: 3 Major Versions Rule for Loader API

**SafeCat only uses loader APIs that have been stable for 3+ major versions.**

Loader-specific code (Forge events, Fabric API, NeoForge events) is the highest-risk part of the
codebase. Loaders break APIs between major Minecraft versions regularly. To avoid constant rewrites:

- An API is considered "safe to use" only if it has not changed in 3+ consecutive major Minecraft
  versions.
- If a loader API was introduced in 1.19, broken in 1.20, and reworked in 1.21 — SafeCat cannot
  use it. Too unstable.
- If an API has been unchanged since 1.17 (→ 1.18 → 1.19 → 1.20 → 1.21), it's stable enough.
- Exceptions require a written rationale in the commit message and approval from at least one other
  maintainer.

### Rule 4: Cross-Version Stability

**A mod written for SafeCat 1.0 MUST work on SafeCat 4.0 without recompilation.**

The API module carries a stability guarantee:

| SafeCat version | API compatibility                          |
|-----------------|--------------------------------------------|
| 1.x             | V1 only                                    |
| 2.x             | V1 (via bridge) + V2                       |
| 3.x             | V1 (via bridge) + V2 + V3                  |
| 4.x             | V2 + V3 + V4                               |
| 5.x             | V3 + V4 + V5                               |

Bridge artifacts (`safecat-bridge-1-to-2`, `safecat-bridge-2-to-3`, etc.) provide the adapters.
SafeCat core auto-wraps old-version providers on load.

Enforcement:

- Removal of an API version happens exactly 3 major releases after deprecation.
- A bridge artifact is removed when the version it bridges from is removed.
- Tests must include at least one provider at the oldest supported API version.

### Rule 5: Deprecate Immediately

**When a new API version ships, the previous version gets `@Deprecated` the same day.**

Timeline:

| Event | Action |
|-------|--------|
| V2 released | `@Deprecated` on all V1 interfaces |
| V3 released | `@Deprecated(forRemoval=true)` on V1 |
| V4 released | V1 removed, bridge `safecat-bridge-1-to-2` deleted |
| V5 released | `@Deprecated(forRemoval=true)` on V2 |
| V6 released | V2 removed, bridge `safecat-bridge-2-to-3` deleted |

A deprecation notice must include:

- The replacement API's fully qualified name
- Migration instructions (or link to `docs/dev/migration-v*-to-v*.md`)
- The expected removal version

---

## Module Structure

```
safecat/
├── api/                    # Pure Java interfaces. Zero deps on Minecraft/loader.
│   └── src/main/java/kz/bejiihiu/safecat/api/
│       ├── Event.java              # Abstract event base
│       ├── EventReason.java        # Categorised reasons for transactions
│       ├── Currency.java           # Immutable currency record
│       ├── CurrencyType.java       # FIAT, DIGITAL, TOKEN, CUSTOM
│       ├── CurrencyProvider.java   # Provider interface (V1)
│       ├── SafeCatAPI.java         # Consumer-facing API singleton (exposes getEventBus())
│       ├── SafeCatEventBus.java    # Custom type-safe event bus (Consumer<T> pattern, not Guava)
│       ├── SafeCatRegistry.java    # Registry for currencies and providers
│       ├── TransactionResult.java  # Transaction outcome record
│       ├── TransactionEvent.java   # Post-transaction event
│       ├── TransactionPriority.java # @Deprecated — unused, kept for reference only
│       ├── BalanceChangeEvent.java # Pre-transaction cancelable event
│       ├── BalanceRequestEvent.java # Pre-balance-lookup event
│       ├── RegisterCurrenciesEvent.java  # Startup registration event
│       ├── RegisterProvidersEvent.java   # Startup registration event
│       ├── CommandRegistrationEvent.java # Startup command registration
│       ├── CommandProvider.java    # Platform-independent command
│       ├── SimpleCommand.java      # Lambda-friendly command builder
│       ├── CommandSender.java      # Generic command sender
│       ├── ChatProvider.java       # Chat formatting provider
│       ├── ChatFormatEvent.java    # Chat format event
│       ├── PermissionProvider.java # Permission provider interface
│       ├── PermissionCheckEvent.java # Permission check event
│       └── codec/                  # Codec interfaces
│           ├── CurrencyCodec.java  # Currency JSON codec
│           ├── TransactionResultCodec.java # TransactionResult JSON codec
│           └── SafeCatGson.java    # Shared Gson instance with Instant/CurrencyType adapters
├── common/                  # SafeCat Core. Implements API, config, event infrastructure.
│   └── src/main/java/kz/bejiihiu/safecat/
│       └── common/
│           ├── SafeCatCore.java         # Bootstrap entry point (creates SafeCatEventBus,
│           │                             # AtomicBoolean guard — idempotent)
│           ├── SafeCatConfig.java       # JSON-based config loader (safecat.json, parsed with Gson)
│           ├── SafeCatRegistryImpl.java # ServiceLoader + event-based registration
│           ├── SafeCatAPIImpl.java      # API implementation + centralized amount validation
│           ├── PlatformHelper.java      # Shared command registration + PlatformCommandSender
│           └── SafecatCommand.java      # Built-in /safecat command (always available)
├── forge/                   # Forge adapter. Forge events ↔ SafeCat API.
├── fabric/                  # Fabric adapter. Fabric API ↔ SafeCat API.
├── neoforge/                # NeoForge adapter. NeoForge events ↔ SafeCat API.
├── provider-numismatic/     # Reference CurrencyProvider implementation
├── example-shop/            # Example consumer mod
├── example-chat/            # Example ChatProvider implementation
├── example-permissions/     # Example PermissionProvider implementation
├── extension-example/       # Example SafeCatExtension — copy to make your own

```

## Module Dependency Graph

```
api              (no deps, pure Java 21)
  ↑
common           (depends only on api)
  ↑
forge fabric neoforge   (depend on common)
```

Each adapter depends on `common` (which depends on `api`). No adapter depends on another adapter.
No adapter depends on `api` directly — it goes through `common`.

### PlatformHelper

`PlatformHelper` lives in `common/` and provides shared command registration logic that's
identical across Forge and NeoForge. Adapters call `PlatformHelper.registerCommands(dispatcher)`
in their `RegisterCommandsEvent` handler instead of duplicating the Brigadier wiring.

`PlatformCommandSender` (inner class) bridges Minecraft's `CommandSourceStack` to SafeCat's
`CommandSender` interface — same implementation on all platforms.

## Vault Comparison Rationale

| Feature | Vault | SafeCat |
|---------|-------|---------|
| Provider discovery | Reflection/instanceof | ServiceLoader + events |
| Reflection in core | Yes | Forbidden (Rule 1) |
| Known implementations | Hardcoded branches | None (Rule 2) |
| Multi-currency | No | First-class Currency type |
| Loader support | Bukkit only | Forge, Fabric, NeoForge |
| Async operations | No | CompletableFuture everywhere |
| Thread safety | Not guaranteed | ConcurrentHashMap, atomic types |
| Deprecation policy | None | 3-major-version guarantee (Rule 4) |
| API stability | Breaking changes at any time | 3 versions of backward compat |
| State | Modifies economy state directly | Pure mediator (Rule 2) |

See [Migration V1 to V2](migration-v1-to-v2.md) for the deprecation timeline and bridge
artifact details.
