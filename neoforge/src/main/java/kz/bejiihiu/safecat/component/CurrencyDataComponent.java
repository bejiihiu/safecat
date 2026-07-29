// Identical to fabric counterpart — keep in sync.
package kz.bejiihiu.safecat.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.CurrencyType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public class CurrencyDataComponent {
  public static final Codec<Currency> MOJANG_CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING.fieldOf("id").forGetter(Currency::id),
                      Codec.STRING.fieldOf("displayName").forGetter(Currency::displayName),
                      Codec.STRING.fieldOf("symbol").forGetter(Currency::symbol),
                      Codec.STRING
                          .xmap(CurrencyType::safeValueOf, CurrencyType::name)
                          .fieldOf("type")
                          .forGetter(Currency::type))
                  .apply(instance, Currency::new));
  public static final DataComponentType<Currency> CURRENCY =
      DataComponentType.<Currency>builder().persistent(MOJANG_CODEC).build();

  public static void applyTo(ItemStack stack, Currency currency) {
    stack.set(CURRENCY, currency);
  }

  private CurrencyDataComponent() {}
}
