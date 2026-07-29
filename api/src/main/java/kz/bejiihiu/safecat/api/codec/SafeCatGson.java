package kz.bejiihiu.safecat.api.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;
import kz.bejiihiu.safecat.api.CurrencyType;

public final class SafeCatGson {
  public static final Gson INSTANCE =
      new GsonBuilder()
          .registerTypeAdapter(Instant.class, new InstantAdapter())
          .registerTypeAdapter(CurrencyType.class, new CurrencyTypeAdapter())
          .create();

  private SafeCatGson() {}

  private static class InstantAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
      out.value(value.toString());
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
      return Instant.parse(in.nextString());
    }
  }

  private static class CurrencyTypeAdapter extends TypeAdapter<CurrencyType> {
    @Override
    public void write(JsonWriter out, CurrencyType value) throws IOException {
      out.value(value.name());
    }

    @Override
    public CurrencyType read(JsonReader in) throws IOException {
      return CurrencyType.safeValueOf(in.nextString());
    }
  }
}
