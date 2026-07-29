package com.example.shop.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.EventReason;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import kz.bejiihiu.safecat.api.TransactionResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code /shop buy|sell <item> [amount]} — example shop transaction.
 *
 * <p><b>Buy</b>: withdraws money via SafeCatAPI, then gives the item. <b>Sell</b>: removes item
 * from inventory, then deposits money via SafeCatAPI.
 *
 * <p>Every economy call goes through {@code SafeCatAPI.getInstance()}. The mod has <em>no
 * compile-time dependency</em> on any specific economy mod.
 */
public final class ShopCommand {

  // Example prices — real mod would load from config.
  private static final Map<String, BigDecimal> PRICES =
      Map.of(
          "diamond", new BigDecimal("50.00"),
          "emerald", new BigDecimal("10.00"),
          "iron_ingot", new BigDecimal("5.00"),
          "gold_ingot", new BigDecimal("20.00"));

  private ShopCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("shop")
            .then(
                Commands.literal("buy")
                    .then(
                        Commands.argument("item", StringArgumentType.word())
                            .executes(ctx -> buy(ctx, 1))
                            .then(
                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(
                                        ctx ->
                                            buy(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))
            .then(
                Commands.literal("sell")
                    .then(
                        Commands.argument("item", StringArgumentType.word())
                            .executes(ctx -> sell(ctx, 1))
                            .then(
                                Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(
                                        ctx ->
                                            sell(
                                                ctx,
                                                IntegerArgumentType.getInteger(ctx, "amount")))))));
  }

  /** /shop buy <item> [amount] — withdraw + give item */
  private static int buy(CommandContext<CommandSourceStack> ctx, int amount)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var player = source.getPlayerOrException();
    String itemName = StringArgumentType.getString(ctx, "item");

    // Resolve item from registry by path name (e.g. "diamond" → minecraft:diamond).
    var itemKey =
        ForgeRegistries.ITEMS.getKeys().stream()
            .filter(key -> key.getPath().equals(itemName))
            .findFirst()
            .orElse(null);
    if (itemKey == null) {
      source.sendFailure(Component.literal("Unknown item: " + itemName));
      return 0;
    }
    var itemEntry = ForgeRegistries.ITEMS.getValue(itemKey);
    if (itemEntry == null) {
      source.sendFailure(Component.literal("Unknown item: " + itemName));
      return 0;
    }

    // Look up price.
    BigDecimal unitPrice = PRICES.get(itemName);
    if (unitPrice == null) {
      source.sendFailure(Component.literal("Item not sold here: " + itemName));
      return 0;
    }

    BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(amount));
    String currencyId = BalanceCommand.firstCurrencyId();
    if (currencyId == null) {
      source.sendFailure(Component.literal("No economy backend installed"));
      return 0;
    }

    // Withdraw money first, then give item on success.
    CompletableFuture<TransactionResult> future =
        SafeCatAPI.getInstance()
            .withdraw(player.getUUID(), currencyId, total, EventReason.SHOP_PURCHASE);

    TransactionResult result = future.join();

    if (result.success()) {
      player.addItem(new ItemStack(itemEntry, amount));
      source.sendSuccess(
          () ->
              Component.literal(
                  "Bought x"
                      + amount
                      + " "
                      + itemName
                      + " for "
                      + total.toPlainString()
                      + " ["
                      + currencyId
                      + "]"),
          false);
    } else {
      source.sendFailure(Component.literal("Purchase failed: " + result.message()));
    }

    return Command.SINGLE_SUCCESS;
  }

  /** /shop sell <item> [amount] — remove item + deposit */
  private static int sell(CommandContext<CommandSourceStack> ctx, int amount)
      throws CommandSyntaxException {
    var source = ctx.getSource();
    var player = source.getPlayerOrException();
    String itemName = StringArgumentType.getString(ctx, "item");

    var itemKey =
        ForgeRegistries.ITEMS.getKeys().stream()
            .filter(key -> key.getPath().equals(itemName))
            .findFirst()
            .orElse(null);
    if (itemKey == null) {
      source.sendFailure(Component.literal("Unknown item: " + itemName));
      return 0;
    }
    var itemEntry = ForgeRegistries.ITEMS.getValue(itemKey);
    if (itemEntry == null) {
      source.sendFailure(Component.literal("Unknown item: " + itemName));
      return 0;
    }

    BigDecimal unitPrice = PRICES.get(itemName);
    if (unitPrice == null) {
      source.sendFailure(Component.literal("We don't buy that: " + itemName));
      return 0;
    }

    // Check player inventory has enough.
    int held = player.getInventory().countItem(itemEntry);
    if (held < amount) {
      source.sendFailure(
          Component.literal("You only have " + held + " x " + itemName + " (need " + amount + ")"));
      return 0;
    }

    // Remove items from inventory.
    int toRemove = amount;
    for (var invStack : player.getInventory().items) {
      if (invStack.is(itemEntry)) {
        int removed = Math.min(toRemove, invStack.getCount());
        invStack.shrink(removed);
        toRemove -= removed;
        if (toRemove == 0) break;
      }
    }

    BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(amount));
    String currencyId = BalanceCommand.firstCurrencyId();
    if (currencyId == null) {
      source.sendFailure(Component.literal("No economy backend installed"));
      return 0;
    }

    // Deposit money after removing items.
    CompletableFuture<TransactionResult> future =
        SafeCatAPI.getInstance()
            .deposit(player.getUUID(), currencyId, total, EventReason.SHOP_PURCHASE);

    TransactionResult result = future.join();

    if (result.success()) {
      source.sendSuccess(
          () ->
              Component.literal(
                  "Sold x"
                      + amount
                      + " "
                      + itemName
                      + " for "
                      + total.toPlainString()
                      + " ["
                      + currencyId
                      + "]"),
          false);
    } else {
      // Rare: deposit failed — put items back.
      player.addItem(new ItemStack(itemEntry, amount));
      source.sendFailure(
          Component.literal("Sell failed: " + result.message() + " — items returned"));
    }

    return Command.SINGLE_SUCCESS;
  }
}
