package kz.bejiihiu.safecat.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import kz.bejiihiu.safecat.api.CommandProvider;
import kz.bejiihiu.safecat.api.CommandSender;
import kz.bejiihiu.safecat.api.SafeCatAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformHelper {

  private PlatformHelper() {}

  /** Shared command registration — identical across all platforms. */
  public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
    for (CommandProvider cmd : SafeCatAPI.getInstance().getCommands()) {
      dispatcher.register(
          Commands.literal(cmd.getName())
              .requires(source -> cmd.getPermission() == null || source.hasPermission(2))
              .executes(ctx -> run(cmd, ctx, ""))
              .then(
                  Commands.argument("args", StringArgumentType.greedyString())
                      .executes(ctx -> run(cmd, ctx, StringArgumentType.getString(ctx, "args")))));
    }
  }

  private static int run(
      CommandProvider cmd, CommandContext<CommandSourceStack> ctx, String joined) {
    CommandSender sender = new PlatformCommandSender(ctx.getSource());
    String[] args = joined.isEmpty() ? new String[0] : joined.split(" ");
    boolean ok = cmd.execute(sender, args).join();
    return ok ? Command.SINGLE_SUCCESS : 0;
  }

  /** Shared CommandSender — identical across all platforms. */
  public static final class PlatformCommandSender implements CommandSender {
    private final CommandSourceStack source;

    PlatformCommandSender(CommandSourceStack source) {
      this.source = source;
    }

    @Override
    public UUID getUniqueId() {
      if (source.getEntity() instanceof ServerPlayer player) {
        return player.getUUID();
      }
      return new UUID(0, 0);
    }

    @Override
    public String getName() {
      return source.getTextName();
    }

    @Override
    public boolean hasPermission(String permission) {
      // console (zero UUID) and ops have all permissions
      var id = getUniqueId();
      if (id.getMostSignificantBits() == 0 && id.getLeastSignificantBits() == 0) {
        return true;
      }
      if (source.hasPermission(2)) {
        return true;
      }
      return SafeCatAPI.getInstance().hasPermission(id, permission).join();
    }

    @Override
    public void sendMessage(String message) {
      source.sendSuccess(() -> Component.literal(message), false);
    }
  }
}
