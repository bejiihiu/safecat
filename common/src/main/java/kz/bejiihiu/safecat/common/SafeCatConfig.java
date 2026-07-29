package kz.bejiihiu.safecat.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SafeCatConfig {

  private static final Logger LOG = LoggerFactory.getLogger(SafeCatConfig.class);

  private static final String DEFAULT_CONFIG = "/safecat.json";

  private final String defaultCurrencyId;
  private final Map<String, BigDecimal> exchangeRates;
  private final boolean rateAutoUpdate;

  SafeCatConfig(
      String defaultCurrencyId, Map<String, BigDecimal> exchangeRates, boolean rateAutoUpdate) {
    this.defaultCurrencyId = defaultCurrencyId;
    this.exchangeRates = Collections.unmodifiableMap(new HashMap<>(exchangeRates));
    this.rateAutoUpdate = rateAutoUpdate;
  }

  public static SafeCatConfig load() {
    return loadDefault();
  }

  private static SafeCatConfig loadDefault() {
    try (InputStream in = SafeCatConfig.class.getResourceAsStream(DEFAULT_CONFIG)) {
      if (in == null) {
        LOG.warn("default config {} not found, using defaults", DEFAULT_CONFIG);
        return fallbackDefaults();
      }
      return parse(JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject());
    } catch (IOException e) {
      LOG.warn("failed to load default config, using fallback defaults", e);
      return fallbackDefaults();
    }
  }

  static SafeCatConfig parse(JsonObject json) {
    JsonElement defaultCurrencyEl = json.get("default-currency");
    String defaultCurrency =
        defaultCurrencyEl != null && !defaultCurrencyEl.isJsonNull()
            ? defaultCurrencyEl.getAsString()
            : "safecat:coin";
    JsonElement autoUpdateEl = json.get("rate-auto-update");
    boolean autoUpdate =
        autoUpdateEl != null && !autoUpdateEl.isJsonNull() ? autoUpdateEl.getAsBoolean() : true;

    Map<String, BigDecimal> rates = new HashMap<>();
    JsonObject exchangeNode = json.getAsJsonObject("exchange-rates");
    if (exchangeNode != null) {
      for (var entry : exchangeNode.entrySet()) {
        try {
          rates.put(entry.getKey(), BigDecimal.valueOf(entry.getValue().getAsDouble()));
        } catch (NumberFormatException e) {
          LOG.warn("invalid exchange rate for {}: {}", entry.getKey(), entry.getValue());
        }
      }
    }

    return new SafeCatConfig(defaultCurrency, rates, autoUpdate);
  }

  private static SafeCatConfig fallbackDefaults() {
    return new SafeCatConfig("safecat:coin", Map.of(), true);
  }

  public String defaultCurrencyId() {
    return defaultCurrencyId;
  }

  public Map<String, BigDecimal> exchangeRates() {
    return exchangeRates;
  }

  public boolean rateAutoUpdate() {
    return rateAutoUpdate;
  }
}
