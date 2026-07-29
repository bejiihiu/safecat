# SafeCat Configuration

- [File Location](#file-location)
- [Format](#format)
- [Options](#options)
  - [Default Currency](#default-currency)
  - [Exchange Rates](#exchange-rates)
  - [Rate Auto Update](#rate-auto-update)
- [Extension Configuration](#extension-configuration)
- [See Also](#see-also)

---

## File Location

SafeCat ships with a built-in default config embedded in the jar (`/safecat.json`). The file is read from the classpath during `SafeCatCore.initialize()`.

**Override path:** `config/safecat.json` (in the server directory). If this file exists, it takes precedence over the embedded default.

**Automatic creation:** The file is not auto-created — you must create it manually if you want to override defaults. SafeCat falls back to built-in defaults if the file doesn't exist.

---

## Format

SafeCat uses standard JSON. All fields are optional — missing fields use their defaults.

### Minimal Example

```json
{
  "default-currency": "safecat:coin"
}
```

### Full Example

```json
{
  "default-currency": "safecat:coin",
  "rate-auto-update": true,
  "exchange-rates": {
    "safecat:coin": 1.0,
    "numismatic:coin": 0.8,
    "villager:emerald": 0.5
  }
}
```

### Embedded Default

The built-in default (`/safecat.json`) contains:

```json
{
  "default-currency": "safecat:coin",
  "rate-auto-update": true,
  "exchange-rates": {
    "safecat:coin": 1.0
  }
}
```

---

## Options

### `default-currency`

```json
{
  "default-currency": "safecat:coin"
}
```

| Aspect | Value |
|--------|-------|
| **Type** | `string` |
| **Default** | `"safecat:coin"` |
| **Required** | No |

The currency identifier used when a consumer calls `SafeCatAPI` without specifying a currency. Must match a currency `id` registered by some `CurrencyProvider`.

**Usage:** Consumer mods call `api.getBalance(player)` without a currency ID — SafeCat uses the default.

**Naming convention:** Use `modid:currencyname` format (e.g., `ftbmoney:coin`, `numismatic:coin`).

### `exchange-rates`

```json
{
  "exchange-rates": {
    "safecat:coin": 1.0,
    "numismatic:coin": 0.8
  }
}
```

| Aspect | Value |
|--------|-------|
| **Type** | `map<string, number>` |
| **Default** | `{"safecat:coin": 1.0}` |
| **Required** | No |

Define conversion rates between currencies. The key is a currency ID, the value is the multiplier relative to the base unit.

**Storage:** Rates are stored in an unmodifiable `Map<String, BigDecimal>` and exposed via `SafeCatConfig.exchangeRates()`.

**Consumption:** SafeCat core only stores the rates — consumer mods call `config.exchangeRates()` to get the map and apply conversions themselves.

**Rate calculation:** `amountInCurrencyA * rateOfCurrencyB / rateOfCurrencyA` gives the equivalent in currency B.

### `rate-auto-update`

```json
{
  "rate-auto-update": true
}
```

| Aspect | Value |
|--------|-------|
| **Type** | `boolean` |
| **Default** | `true` |
| **Required** | No |

When `true`, SafeCat may poll external data sources for live exchange rates (e.g. for DIGITAL currency types). Set to `false` to use only manually configured `exchange-rates` values.

**Note:** The auto-update mechanism is a placeholder for future implementation. Currently, rates are static between config reloads regardless of this setting.

---

## Extension Configuration

Extensions are configured separately — see [Integrations](../dev/integrations.md) for details.

**Extension directory:** `config/safecat/extensions/`

**Auto-download:** On first startup, SafeCat downloads available extensions from the latest GitHub release into this directory.

```
config/safecat/
├── extensions/
│   └── safecat-extension-luckperms.jar
└── safecat.json
```

---

## See Also

- [Setup Guide](setup.md) — installation and first run
- [Troubleshooting](troubleshooting.md) — common config issues
- [Provider Development](../dev/provider.md) — implementing a CurrencyProvider
