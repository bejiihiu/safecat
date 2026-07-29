package kz.bejiihiu.safecat.api.codec;

import com.google.gson.TypeAdapter;
import kz.bejiihiu.safecat.api.TransactionResult;

public final class TransactionResultCodec {
  public static final TypeAdapter<TransactionResult> INSTANCE =
      SafeCatGson.INSTANCE.getAdapter(TransactionResult.class);

  private TransactionResultCodec() {}
}
