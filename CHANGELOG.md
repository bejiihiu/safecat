# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] — 2026-07-29

### Added
- Built-in `/safecat` command (help, status, currencies)
- JSON config format (replaced .properties)
- SafeCatEventBus — minimal typed event bus (replaced Guava)
- PlatformHelper — shared command registration across Fabric/Forge/NeoForge
- Proper thread-safety guards in SafeCatCore
- Test isolation support (resetInstance/reset)

### Changed
- Permission checks now route through SafeCat permission providers
- Logging migrated from JUL to SLF4J
- Provider amount validation centralized in API layer
- BridgeEvent simplified — no unsafe casts, no reflection
- Config format changed from .properties to JSON

### Fixed
- Critical: CurrencyProvider double-registration (double-spend risk)
- Critical: Config resource not found silently falling back to defaults
- UUID zero constant parsed on every transaction
- ChatProvider priority default now consistent (0 vs 1)
- Test pollution from global singleton

### Removed
- Guava EventBus dependency
- Common BridgeEvent middleman layer
- TransactionPriority enum (deprecated, unused)
- java.util.logging usage
- .properties config format

### Security
- CommandSender.hasPermission() now delegates to SafeCat permission chain

## [Unreleased]

### Added

- Initial project structure: `api`, `common`, `forge`, `fabric`, `neoforge` modules
- Pure Java API module with zero Minecraft dependencies
- ServiceLoader-based provider discovery (no reflection, no instanceof)
- Pure mediator pattern with `TransactionPriority` chain (EARLIEST → NORMAL → LATE)
- `CompletableFuture`-based async provider methods
- First-class `Currency` type with FIAT, DIGITAL, TOKEN, CUSTOM types
- Forge adapter module
- Fabric adapter module
- NeoForge adapter module
- Cross-version stability guarantee (3 major versions of backward compatibility)
- Thread-safe core data structures
- Spotless-based code formatting (Google Java Format)
- GitHub templates: bug report, feature request, PR template
- Community standards: CONTRIBUTING, SECURITY, CODE_OF_CONDUCT
- AGPL v3 license
