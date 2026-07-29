package kz.bejiihiu.safecat.codec;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import kz.bejiihiu.safecat.api.TransactionResult;
import kz.bejiihiu.safecat.api.codec.TransactionResultCodec;
import org.junit.jupiter.api.Test;

class TransactionResultCodecTest {

  @Test
  void roundtripSuccess() throws IOException {
    TransactionResult original = TransactionResult.success(BigDecimal.TEN, "test:coin");
    String json = TransactionResultCodec.INSTANCE.toJson(original);
    TransactionResult decoded = TransactionResultCodec.INSTANCE.fromJson(json);
    assertEquals(original.success(), decoded.success());
    assertEquals(0, original.amount().compareTo(decoded.amount()));
    assertEquals(original.currencyId(), decoded.currencyId());
    assertEquals(original.message(), decoded.message());
  }

  @Test
  void roundtripFailure() throws IOException {
    TransactionResult original = TransactionResult.failure("test:coin", "insufficient funds");
    String json = TransactionResultCodec.INSTANCE.toJson(original);
    TransactionResult decoded = TransactionResultCodec.INSTANCE.fromJson(json);
    assertEquals(original.success(), decoded.success());
    assertEquals(original.currencyId(), decoded.currencyId());
    assertEquals(original.message(), decoded.message());
  }

  @Test
  void decodeMissingOrInvalid_throws() {
    assertThrows(
        RuntimeException.class,
        () -> sneaky(TransactionResultCodec.INSTANCE::fromJson, "not json"));
    assertThrows(
        RuntimeException.class,
        () -> sneaky(TransactionResultCodec.INSTANCE::fromJson, "{\"success\":true}"));
    assertThrows(
        RuntimeException.class,
        () ->
            sneaky(
                TransactionResultCodec.INSTANCE::fromJson,
                "{\"success\":true,\"amount\":null,\"currencyId\":\"test\",\"message\":\"ok\",\"timestamp\":\"2024-01-01T00:00:00Z\"}"));
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
