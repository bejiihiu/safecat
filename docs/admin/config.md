# SafeCat Configuration

- [File Location](#file-location)
- [Format](#format)
- [Options](#options)
  - [Default Currency](#default-currency)
  - [Exchange Rates](#exchange-rates)
  - [Rate Auto Update](#rate-auto-update)
- [See Also](#see-also)

---

## File Location

SafeCat ships with a built-in default config embedded in the jar (`/safecat.json`). You can override it at runtime by calling `SafeCatConfig.load(Path)` with a path to your own file:

```java
var config = SafeCatConfig.load(Path.of("config", "safecat.json"));
```

The standard convention is to place the override at `config/safecat.json` in the server directory. If the file doesn't exist or can't be loaded, SafeCat falls back to built-in defaults — all options are optional.

## Format

SafeCat uses JSON syntax. Example:

```json
{
  "default-currency": "safecat:coin",
  "rate-auto-update": true,
  "exchange-rates": {
    "safecat:coin": 1.0,
    "villager:emerald": 0.5
  }
}
```

## Options

### Default Currency

```json
{
  "default-currency": "safecat:coin"
}
```

**Type:** string  
**Default:** `safecat:coin`

The currency identifier used when a consumer calls `SafeCatAPI` without specifying a currency. Must match a currency `id` registered by a `CurrencyProvider`.

### Exchange Rates

```json
{
  "exchange-rates": {
    "safecat:coin": 1.0,
    "villager:emerald": 0.5
  }
}
```

**Type:** map of currency ID → decimal  
**Default:** empty

Define conversion rates between currencies. The key is a currency ID, the value is the multiplier relative to the base unit (typically 1.0 for the primary currency).

Rates are read during config load and exposed via `SafeCatConfig.exchangeRates()`. How they're applied is up to the consumer — SafeCat core only stores them.

### Rate Auto Update

```json
{
  "rate-auto-update": true
}
```

**Type:** boolean  
**Default:** `true`

When `true`, SafeCat polls external data sources for live exchange rates (e.g. for DIGITAL currencies). Set to `false` to use only manually configured `exchange-rates` values.

## See Also

- [Setup Guide](setup.md) — installing SafeCat
- [Troubleshooting](troubleshooting.md) — common config issues
- [Provider Development](../dev/provider.md) — implementing a CurrencyProvider
