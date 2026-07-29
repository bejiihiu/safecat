package kz.bejiihiu.safecat.codec;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyType;
import kz.bejiihiu.safecat.api.codec.CurrencyCodec;
import org.junit.jupiter.api.Test;

class CurrencyCodecTest {

  @Test
  void roundtrip() throws IOException {
    Currency original = new Currency("test:coin", "Test Coin", "T", CurrencyType.TOKEN);
    String json = CurrencyCodec.INSTANCE.toJson(original);
    Currency decoded = CurrencyCodec.INSTANCE.fromJson(json);
    assertEquals(original, decoded);
  }

  @Test
  void roundtripCustomType() throws IOException {
    Currency original = new Currency("minecraft:emerald", "Emerald", "E", CurrencyType.CUSTOM);
    String json = CurrencyCodec.INSTANCE.toJson(original);
    Currency decoded = CurrencyCodec.INSTANCE.fromJson(json);
    assertEquals(original, decoded);
  }

  @Test
  void decodeMissingOrInvalid_throws() {
    assertThrows(
        RuntimeException.class, () -> sneaky(CurrencyCodec.INSTANCE::fromJson, "not json"));
    assertThrows(
        RuntimeException.class,
        () -> sneaky(CurrencyCodec.INSTANCE::fromJson, "{\"id\":\"test\"}"));
    assertThrows(
        RuntimeException.class,
        () ->
            sneaky(
                CurrencyCodec.INSTANCE::fromJson,
                "{\"id\":null,\"displayName\":\"Test\",\"symbol\":\"T\",\"type\":\"TOKEN\"}"));
  }

  private static <T> T sneaky(ThrowingFunction<T> fn, String arg) {
    try {
      return fn.apply(arg);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FunctionalInterface
  private interface ThrowingFunction<T> {
    T apply(String arg) throws IOException;
  }
}
