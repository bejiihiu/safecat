# Integrations

SafeCat ships an **extension system** — third-party mods can integrate with SafeCat
by dropping an extension JAR into `config/safecat/extensions/`.

Extensions are loaded at startup by `ExtensionLoader`. Each extension:
- Checks if its target mod is installed at runtime
- If yes, registers the appropriate SafeCat provider(s)
- If no, does nothing (graceful no-op)

## Auto-Download

On **first server startup**, SafeCat automatically downloads available extensions
from the **latest GitHub release** into `config/safecat/extensions/`.

This is handled by `ExtensionDownloader`:
1. Reads `https://api.github.com/repos/bejiihiu/safecat/releases/latest`
2. Finds the extension JAR in the release assets
3. Downloads it to `config/safecat/extensions/<artifact>.jar`
4. `ExtensionLoader` picks it up on the same startup

The download is **one-time** — if the file already exists, it is skipped.
Delete the JAR and restart to force re-download.

## Available Extensions

| Extension | Type | Target Mod | SafeCat Interface | Status |
|-----------|------|------------|-------------------|--------|
| `extension-luckperms` | Permission | LuckPerms | `PermissionProvider` | ✅ Working, real LP API |

## How Extensions Work

1. Place the extension JAR in `config/safecat/extensions/`
2. SafeCat detects it on next startup via `ServiceLoader`
3. The extension calls `SafeCatAPI.getInstance().registerAdapter(...)` to register providers
4. That's it — no config, no commands, no setup

### Building Extensions

```bash
# Build all extensions
./gradlew :extension-luckperms:build

# JAR is in extension-luckperms/build/libs/safecat-extension-luckperms-*.jar
```

## Writing Your Own Extension

Extensions are simple. Create a JAR containing:

1. A class implementing `kz.bejiihiu.safecat.extension.SafeCatExtension`:

```java
package com.example.myextension;

import kz.bejiihiu.safecat.api.SafeCatAPI;
import kz.bejiihiu.safecat.extension.SafeCatExtension;

public class MyExtension implements SafeCatExtension {

  @Override
  public String id() {
    return "my-eco-mod";
  }

  @Override
  public String name() {
    return "My Economy Mod Integration";
  }

  @Override
  public void init() {
    // 1. Check if target mod is loaded (Class.forName, isModLoaded, etc.)
    // 2. If not loaded → return (no-op)
    // 3. If loaded → create providers → registerAdapter()
    SafeCatAPI.getInstance().registerAdapter(new MyCurrencyProvider());
  }
}
```

2. A `META-INF/services/kz.bejiihiu.safecat.extension.SafeCatExtension` file
   containing the fully qualified class name

3. Dependencies (compile against SafeCat API, your target mod's API, and SLF4J)

### build.gradle

```groovy
plugins {
    id 'java-library'
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'kz.bejiihiu:safecat-api:1.0.0'
    compileOnly 'org.slf4j:slf4j-api:2.0.9'
    // Your mod's API:
    compileOnly 'net.example:some-mod-api:1.0'
}
```

## Extension vs Provider

| | Extension | Provider |
|---|---|---|
| **Deploy** | Drop JAR in `config/safecat/extensions/` | Drop JAR in `mods/` |
| **Discovery** | ExtensionLoader → ServiceLoader | Minecraft mod loader → ServiceLoader |
| **Target mod** | Checks at runtime, graceful no-op | Requires target mod at compile time |
| **Best for** | Third-party integrations | Built-in or first-party providers |
