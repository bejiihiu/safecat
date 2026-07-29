# SafeCat Setup Guide

- [Requirements](#requirements)
- [Installation](#installation)
- [First Run](#first-run)
- [Verification](#verification)
- [Extensions](#extensions)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)

---

## Requirements

| Dependency       | Version      | Notes                                   |
|------------------|--------------|------------------------------------------|
| Java             | 21+          | OpenJDK or Oracle JDK 21                |
| Minecraft        | 1.21         | Forge 51, Fabric 0.100, NeoForge 21     |
| Mod loader       | Any          | Forge, Fabric, or NeoForge              |

SafeCat ships as a multi-loader mod — download the right jar for your loader.

### Supported Loaders

| Loader    | File Pattern                  |
|-----------|-------------------------------|
| Forge     | `safecat-forge-*.jar`         |
| Fabric    | `safecat-fabric-*.jar`        |
| NeoForge  | `safecat-neoforge-*.jar`      |

Jars are **not interchangeable** across loaders.

---

## Installation

### Server Setup

1. **Install Java 21** if missing:
   ```bash
   # Linux (Adoptium)
   apt install temurin-21-jdk
   # Verify
   java -version
   # → openjdk version "21" 2023-09-19 LTS
   ```

2. **Place SafeCat** in `mods/`:
   ```
   mods/
   ├── safecat-forge-*.jar       # Forge server
   └── your-economy-mod.jar      # e.g., Numismatic Overhaul
   ```

3. **Start the server.** SafeCat logs its startup:
   ```
   [main/INFO] [SafeCat] Initializing SafeCat Core...
   [main/INFO] [SafeCat] Loaded config from /safecat.json
   [main/INFO] [SafeCat] Registered provider: NumismaticProvider (numismatic:coin, prio=0)
   [main/INFO] [SafeCat] Registered currency: numismatic:coin (Numismatic Coin, Ⓝ)
   [main/INFO] [SafeCat] SafeCat initialized successfully
   ```

### Client

No client installation required. Players do not need SafeCat installed.

---

## First Run

SafeCat creates the following files on first startup:

| Path | Purpose |
|------|---------|
| `config/safecat.json` | Configuration (auto-created with defaults if missing) |
| `config/safecat/extensions/` | Extension directory (created, may remain empty) |
| `config/safecat/extensions/safecat-extension-luckperms.jar` | LuckPerms extension (auto-downloaded from latest GitHub release) |

### Extension Auto-Download

On first startup, SafeCat checks `config/safecat/extensions/` for extensions and **automatically downloads missing extensions** from GitHub Releases:

| Extension | Downloaded If Missing | Purpose |
|-----------|----------------------|---------|
| `safecat-extension-luckperms.jar` | Yes | Bridges LuckPerms permissions to SafeCat's `PermissionProvider` API |

The download happens once. If the file already exists, it is not re-downloaded. To force re-download, delete the JAR and restart.

Download URL: `https://api.github.com/repos/bejiihiu/safecat/releases/latest` (fetched dynamically).

---

## Verification

### Quick Health Check

```bash
# Run from server console or in-game:
/safecat status
```

Expected output:
```
[SafeCat] Currencies: 1
[SafeCat] Config loaded: true
[SafeCat] Event bus: active
```

### Detailed Checks

| Check | Command | What To Look For |
|-------|---------|------------------|
| List currencies | `/safecat currencies` | Registered currencies with IDs and symbols |
| Extension loaded | Check `config/safecat/extensions/` | JAR files present |
| LuckPerms integration | `/lp sync` then check perms | Permissions work through SafeCat |
| Provider registered | Server log at startup | `[SafeCat] Registered provider: ...` |
| No providers warning | Server log | `WARNING: No CurrencyProvider implementations found` (only if no economy mod installed) |

### Integration Check (LuckPerms)

If LuckPerms is installed on the server:

1. The extension JAR is auto-downloaded to `config/safecat/extensions/safecat-extension-luckperms.jar`
2. On next startup, SafeCat loads it and registers `LuckPermsExtension` as a `PermissionProvider`
3. Verify: run a command that uses `SafeCatAPI.hasPermission()` — it should reflect LuckPerms groups

---

## Extensions

SafeCat has an extension system for third-party integrations. Extensions are JARs placed in `config/safecat/extensions/` and loaded at startup.

### Available Extensions

| Extension | Type | Target Mod | Provides | Auto-Download |
|-----------|------|------------|----------|---------------|
| `extension-luckperms` | Permission | LuckPerms | `PermissionProvider` | ✅ (on first run) |

### Manual Extension Installation

```bash
# 1. Download the extension JAR from GitHub Releases:
curl -L https://github.com/bejiihiu/safecat/releases/latest/download/safecat-extension-luckperms-1.0.jar \
  -o config/safecat/extensions/safecat-extension-luckperms.jar

# 2. Restart the server
```

### Extension Directory Structure

```
config/
└── safecat/
    ├── extensions/
    │   └── safecat-extension-luckperms.jar
    └── safecat.json
```

---

## Permissions

See the [Permissions Reference](permissions.md) for all permission nodes and default levels.

---

## Troubleshooting

See the [Troubleshooting FAQ](troubleshooting.md) for common issues.
