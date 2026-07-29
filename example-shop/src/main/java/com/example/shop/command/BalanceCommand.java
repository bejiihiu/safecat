package com.example.shop.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /balance} — shows the player's balance in the first available currency.
 *
 * <p>Pattern: grab the first registered currency (or let the player choose), call {@code
 * SafeCatAPI.getBalance()}, display the result. Works with any economy backend — SafeCat resolves
 * the CurrencyProvider internally.
 */
public final class BalanceCommand {

  private BalanceCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("balance")
            .executes(
                ctx -> {
                  var source = ctx.getSource();
                  var player = source.getPlayerOrException();

                  // Resolve the first available currency — "any economy mod" mode.
                  String currencyId = firstCurrencyId();
                  if (currencyId == null) {
                    source.sendFailure(
                        Component.literal(
                            "No economy backend installed. Install SafeCat + any CurrencyProvider."));
                    return 0;
                  }

                  // SafeCatAPI is the only import a consumer mod needs.
                  CompletableFuture<BigDecimal> future =
                      SafeCatAPI.getInstance().getBalance(player.getUUID(), currencyId);
                  BigDecimal balance = future.join();

                  source.sendSuccess(
                      () ->
                          Component.literal(
                              "Balance: " + balance.toPlainString() + " [" + currencyId + "]"),
                      false);
                  return Command.SINGLE_SUCCESS;
                }));
  }

  /** Returns the id of the first registered currency, or null if none. */
  static String firstCurrencyId() {
    var currencies = SafeCatAPI.getInstance().getCurrencies();
    return currencies.isEmpty() ? null : currencies.iterator().next().id();
  }
}
