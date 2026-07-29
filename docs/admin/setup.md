# SafeCat Setup Guide

- [Requirements](#requirements)
- [Installation](#installation)
- [Verification](#verification)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)

---

## Requirements

| Dependency       | Version      | Notes                                   |
|------------------|--------------|------------------------------------------|
| Java             | 21+          | OpenJDK or Oracle JDK 21                |
| Minecraft        | 1.21         | Forge 51, Fabric 0.100, NeoForge 21     |
| Mod loader       | Any          | Forge, Fabric, or NeoForge              |
| Economy mod      | Any          | Any mod implementing `CurrencyProvider` |

SafeCat ships as a multi-loader mod — download the right jar for your loader. All loaders are supported equally; pick whichever your server ecosystem uses.

## Installation

### Server

1. **Install Java 21** if you don't have it already:
   ```
   # Linux (Adoptium)
   apt install temurin-21-jdk

   # Windows — download from https://adoptium.net
   ```
   Verify: `java -version` must show `openjdk version "21"` or higher.

2. **Drop SafeCat in `mods/`** — choose the right jar for your loader:
   - `safecat-forge-*.jar` → Forge
   - `safecat-fabric-*.jar` → Fabric
   - `safecat-neoforge-*.jar` → NeoForge

   The version number in the filename is generated from commit count during CI builds.

3. **Drop your economy mod** (e.g. FTB Money, Numismatic Overhaul, or a custom provider) in `mods/`.

4. (Optional) Create a config file at `config/safecat.json` — see [Config Reference](config.md).

5. Start the server. SafeCat logs provider registration during startup:
   ```
   [SafeCat] Registered provider: NumismaticProvider (numismatic:coin, priority=0)
   [SafeCat] Registered currency: numismatic:coin (Numismatic Coin, Ⓝ)
   ```

### Client

No client installation is required for SafeCat itself. Players connecting to a SafeCat-enabled server do not need the mod. Economy mods that have client-side components (like balance HUDs) should be installed on the client per their instructions.

## Verification

Run these checks after installation:

1. **Run `/safecat status`** — shows loaded providers, registered currencies, and config status. Run it in-game or from server console.

2. **Run `/safecat currencies`** — lists all registered currencies with their IDs and symbols.

3. **Test a balance lookup** — if your economy mod provides a command, check that it returns a balance. Otherwise install a consumer mod (like Example Shop) that calls `SafeCatAPI.getBalance()` and verify it works.

4. **Check server log** for SafeCat startup diagnostics:
   ```
   [SafeCat] Registered provider: ...
   [SafeCat] WARNING: No CurrencyProvider implementations found. Install an economy mod!
   ```

## Permissions

See the [Permissions Reference](permissions.md) for all permission nodes and default levels.

## Troubleshooting

See the [Troubleshooting FAQ](troubleshooting.md) for common issues.
