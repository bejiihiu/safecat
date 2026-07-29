package com.example.shop;

import com.example.shop.command.BalanceCommand;
import com.example.shop.command.ShopCommand;
import com.example.shop.listener.CancelTransactionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Main entry point for Example Shop.
 *
 * <p>This is a Forge-only example that shows how a mod uses the SafeCat API. It registers two
 * commands ({@code /shop}, {@code /balance}) and listens for {@code BalanceChangeEvent} to
 * demonstrate transaction gating.
 *
 * <p><b>Required dependencies at runtime:</b>
 *
 * <ul>
 *   <li>SafeCat (any loader) — provides the economy API + event bus
 *   <li>Any installed CurrencyProvider (e.g. FTB Money, Numismatic, or a custom one)
 * </ul>
 */
@Mod(ExampleShop.MOD_ID)
public class ExampleShop {

  static final String MOD_ID = "example_shop";

  public ExampleShop() {
    // Register Forge commands (reacts to RegisterCommandsEvent).
    MinecraftForge.EVENT_BUS.register(this);
    // Safe: registers on SafeCat's event bus only after Safecat init completes.
    CancelTransactionHandler.registerLater();
  }

  @SubscribeEvent
  void onRegisterCommands(RegisterCommandsEvent event) {
    // Example: commands registered the usual Forge way,
    // internal logic calls SafeCatAPI.getInstance() for all economy ops.
    ShopCommand.register(event.getDispatcher());
    BalanceCommand.register(event.getDispatcher());
  }
}
