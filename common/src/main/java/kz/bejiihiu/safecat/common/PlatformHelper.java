package kz.bejiihiu.safecat.common;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
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
              .executes(
                  ctx -> {
                    CommandSender sender = new PlatformCommandSender(ctx.getSource());
                    String[] args = ctx.getInput().split(" ");
                    String[] tail = new String[Math.max(0, args.length - 1)];
                    if (tail.length > 0) System.arraycopy(args, 1, tail, 0, tail.length);
                    boolean ok = cmd.execute(sender, tail).join();
                    return ok ? Command.SINGLE_SUCCESS : 0;
                  }));
    }
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
      return SafeCatAPI.getInstance().hasPermission(getUniqueId(), permission).join();
    }

    @Override
    public void sendMessage(String message) {
      source.sendSuccess(() -> Component.literal(message), false);
    }
  }
}
