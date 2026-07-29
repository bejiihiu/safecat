# Contributing to SafeCat

- [Architecture](#architecture)
- [Development Setup](#development-setup)
- [Project Conventions](#project-conventions)
- [Pull Request Process](#pull-request-process)
- [Deprecation and Compatibility](#deprecation-and-compatibility)
- [AI-Assisted Contributions](#ai-assisted-contributions)

## Architecture

**Read [`docs/dev/architecture.md`](docs/dev/architecture.md) first.** It documents every
architectural rule: no reflection, pure mediator, loader API stability, cross-version
compatibility, and the deprecation policy. These rules also apply to documentation — see `docs/dev/architecture.md` and
`docs/dev/migration-v1-to-v2.md` for the deprecation and compatibility policies.
PRs that violate these rules will be rejected.

## Development Setup

Requirements:

- JDK 21
- Gradle (wrapper included)

Build all modules:

```bash
./gradlew build
```

Run a single module:

```bash
./gradlew :api:build
./gradlew :common:build
./gradlew :forge:build
./gradlew :fabric:build
./gradlew :neoforge:build
```

On Windows, ensure `JAVA_HOME` points to JDK 21:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
```

### Code Style

SafeCat uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format (v1.18.1)
to enforce consistent formatting. Run before committing:

```bash
./gradlew spotlessApply
```

All Java files must compile with `--release 21` (project source compatibilty). No language
features from later JDKs are allowed.

## Project Conventions

1. **Package:** `kz.bejiihiu.safecat` — do not rename, do not split.
2. **No Minecraft deps in api/:** The `api` module must compile on any JDK 21 without
   Minecraft, Forge, Fabric, or NeoForge on the classpath.
3. **ServiceLoader for providers:** Always use `META-INF/services` to register providers.
   Never hardcode provider lookups.
4. **CompletableFuture:** All provider methods that can block must return
   `CompletableFuture<>`. Sync wrappers belong in adapters, not in core.
5. **Thread safety:** Core data structures must be safe for concurrent access
   (`ConcurrentHashMap`, atomic types, immutable records where possible).
6. **JavaDoc** on every public API method. No exceptions.

## Pull Request Process

1. Ensure your branch targets the correct base (usually `main`).
2. Run `./gradlew build` — all modules must compile.
3. Run `./gradlew spotlessApply` — code must be formatted.
4. Add or update tests in the same PR as production changes.
5. If you change an API interface, mark the old method `@Deprecated` with a pointer to the
   replacement **(see [Deprecation Policy](#deprecation-and-compatibility))**. Do not remove
   the old method in the same PR.
6. Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
   `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
7. Large changes must be preceded by a discussion in an issue before code is written.

## Deprecation and Compatibility

SafeCat guarantees **3 major versions of backward compatibility**. See
[`docs/dev/architecture.md`](docs/dev/architecture.md) for the full deprecation timeline.

When deprecating:

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
 * @deprecated scheduled for removal in 4.0, use {@link NewAPI}
 */
@Deprecated(since = "3.0", forRemoval = true)
public interface OldAPI { ... }
```

## AI-Assisted Contributions

We use LLMs and accept PRs written with AI assistance. See README.md for the full policy.
The short version: we don't care how it was written, we care that it's correct.
