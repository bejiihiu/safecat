# Migration: V1 → V2

- [Deprecation Policy](#deprecation-policy)
- [Compatibility Guarantee](#compatibility-guarantee)
- [Bridge Artifact](#bridge-artifact)
- [Migration Path](#migration-path)
- [Writing Deprecation Notices](#writing-deprecation-notices)
- [What Changed in 1.0.0](#what-changed-in-100)

---

## Deprecation Policy

When a new API version ships, the previous version gets `@Deprecated` the same day.

| Event | Action |
|-------|--------|
| V2 released | `@Deprecated` on all V1 interfaces |
| V3 released | `@Deprecated(forRemoval=true)` on V1 |
| V4 released | V1 removed, bridge `safecat-bridge-1-to-2` deleted |
| V5 released | `@Deprecated(forRemoval=true)` on V2 |
| V6 released | V2 removed, bridge `safecat-bridge-2-to-3` deleted |

A deprecation notice must include:

- The replacement API's fully qualified name
- Migration instructions (or link to this document)
- The expected removal version

## Compatibility Guarantee

**A mod written for SafeCat 1.0.0 (V1 API) MUST work on SafeCat 2.0, 3.0, and 4.0 without
recompilation.**

| SafeCat version | API compatibility                          |
|-----------------|--------------------------------------------|
| 1.x             | V1 only                                    |
| 2.x             | V1 (via bridge) + V2                       |
| 3.x             | V1 (via bridge) + V2 + V3                  |
| 4.x             | V2 + V3 + V4                               |
| 5.x             | V3 + V4 + V5                               |
| 6.x             | V4 + V5 + V6                               |

On V4.0 a provider using V1 logs a warning but still functions. On V5.0 loading a V1
provider fails with "update your dependency to V2".

## Bridge Layer

Bridge layer will be added when V2 ships. Until then, only the V1 API exists and no
translation is needed.

## Migration Path

1. **Provider compiled for V1** — works on V2–V4 via auto-wrapping bridge. No changes needed.

2. **To drop the bridge dependency** — update your provider to implement the V2 interfaces
   directly. Change your imports and recompile.

3. **When V4 ships** — V1 and the bridge are removed. Update to V2 before then or your
   provider will fail to load.

## Writing Deprecation Notices

When deprecating an API:

```java
/**
 * Use {@link NewAPI} instead.
 * Migration guide: docs/dev/migration-v1-to-v2.md
 *
 * @deprecated since 2.0, scheduled for removal in 4.0
 */
@Deprecated(since = "2.0", forRemoval = false)
public interface OldAPI { ... }
```

When marking for removal (next major after deprecation):

```java
/**
 * Use {@link NewAPI} instead.
 *
 * @deprecated since 3.0, scheduled for removal in 4.0
 */
@Deprecated(since = "3.0", forRemoval = true)
public interface OldAPI { ... }
```

## What Changed in 1.0.0

| Area | Before | After |
|------|--------|-------|
| Event Bus | Guava EventBus (`@Subscribe` + `register()`) | SafeCatEventBus (`bus.on(Type, handler)`) |
| Bridge Event | BridgeEvent middleman in `common` | ForgeBridgeEvent / NeoForgeBridgeEvent wrap `api.Event` directly in each platform module |
| Config format | `.properties` (safecat.properties) | JSON (`safecat.json`, parsed with Gson) |
| Gson instance | One-off per consumer | `SafeCatGson.INSTANCE` (shared, with Instant/CurrencyType adapters) |
| Amount validation | Each provider validated amounts | Centralized in `SafeCatAPIImpl` — providers can skip check |
| `TransactionPriority` | Active enum | `@Deprecated` — priority expressed as `int` on provider interfaces |
| `ChatProvider.priority()` | Defaulted to `TransactionPriority.NORMAL.ordinal()` | Returns `0` — simpler, no enum dependency |
| Command registration | Each adapter wired Brigadier separately | `PlatformHelper.registerCommands()` — shared logic across Forge/NeoForge |
| `/safecat` command | Not present | Built-in `SafecatCommand` — always available, no permission required |
| `SafeCatCore.initialize()` | No idempotency guard | `AtomicBoolean` guard — safe to call multiple times |
| Testing | No `reset()` | `SafeCatCore.reset()` + `SafeCatAPI.resetInstance()` for clean test state |

## See Also

- [Architecture Rules](architecture.md) — design rationale for the deprecation policy
- [Provider Guide](provider.md) — writing providers for the current API
- [Consumer Guide](consumer.md) — using SafeCatAPI
