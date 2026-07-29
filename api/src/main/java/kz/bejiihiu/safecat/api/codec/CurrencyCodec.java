package kz.bejiihiu.safecat.api.codec;

import com.google.gson.TypeAdapter;
import kz.bejiihiu.safecat.api.Currency;

public final class CurrencyCodec {
  public static final TypeAdapter<Currency> INSTANCE =
      SafeCatGson.INSTANCE.getAdapter(Currency.class);

  private CurrencyCodec() {}
}
