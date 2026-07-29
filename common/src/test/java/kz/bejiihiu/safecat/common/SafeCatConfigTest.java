package kz.bejiihiu.safecat.common;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SafeCatConfigTest {

  @Test
  void loadDefaultConfig() {
    SafeCatConfig config = SafeCatConfig.load();
    assertEquals("safecat:coin", config.defaultCurrencyId());
    assertTrue(config.rateAutoUpdate());
    assertEquals(0, new BigDecimal("1.0").compareTo(config.exchangeRates().get("safecat:coin")));
  }

  @Test
  void loadFromJsonWithCustomValues() {
    JsonObject json = new JsonObject();
    json.addProperty("default-currency", "minecraft:emerald");
    json.addProperty("rate-auto-update", false);
    JsonObject rates = new JsonObject();
    rates.addProperty("minecat:gold", 0.5);
    json.add("exchange-rates", rates);

    SafeCatConfig config = SafeCatConfig.parse(json);
    assertEquals("minecraft:emerald", config.defaultCurrencyId());
    assertFalse(config.rateAutoUpdate());
    assertEquals(0, new BigDecimal("0.5").compareTo(config.exchangeRates().get("minecat:gold")));
  }

  @Test
  void missingValuesUseDefaults() {
    JsonObject json = new JsonObject();
    SafeCatConfig config = SafeCatConfig.parse(json);
    assertEquals("safecat:coin", config.defaultCurrencyId());
    assertTrue(config.rateAutoUpdate());
    assertTrue(config.exchangeRates().isEmpty());
  }

  @Test
  void rateZeroSemantics() {
    JsonObject json = new JsonObject();
    JsonObject rates = new JsonObject();
    rates.addProperty("minecat:blocked", 0.0);
    json.add("exchange-rates", rates);
    SafeCatConfig config = SafeCatConfig.parse(json);
    assertEquals(0, BigDecimal.ZERO.compareTo(config.exchangeRates().get("minecat:blocked")));
  }

  @Test
  void exchangeRatesAreImmutable() {
    JsonObject json = new JsonObject();
    JsonObject rates = new JsonObject();
    rates.addProperty("test", 1.0);
    json.add("exchange-rates", rates);
    SafeCatConfig config = SafeCatConfig.parse(json);
    assertThrows(
        UnsupportedOperationException.class, () -> config.exchangeRates().put("x", BigDecimal.ONE));
  }
}
