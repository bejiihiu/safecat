package kz.bejiihiu.safecat.common;

import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.CommandProvider;
import kz.bejiihiu.safecat.api.CommandSender;
import kz.bejiihiu.safecat.api.SafeCatAPI;

/** Built-in /safecat command — always available, no provider needed. */
public class SafecatCommand implements CommandProvider {

  public SafecatCommand() {
    SafeCatAPI.getInstance().registerCommand(this);
  }

  @Override
  public String getName() {
    return "safecat";
  }

  @Override
  public String getDescription() {
    return "SafeCat mod information";
  }

  @Override
  public String getPermission() {
    return null; // No permission required.
  }

  @Override
  public CompletableFuture<Boolean> execute(CommandSender sender, String[] args) {
    if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
      sender.sendMessage("§6[SafeCat] §eSafeCat v" + getVersion() + " — economy API for Minecraft");
      sender.sendMessage("§7/safecat help §8— this help");
      sender.sendMessage("§7/safecat status §8— show loaded providers and currencies");
      sender.sendMessage("§7/safecat currencies §8— list registered currencies");
      return CompletableFuture.completedFuture(true);
    }

    if (args[0].equals("status")) {
      var api = SafeCatAPI.getInstance();
      var currencies = api.getCurrencies();
      sender.sendMessage("§6[SafeCat] §7Currencies: §e" + currencies.size());

      // Just show basic status.
      sender.sendMessage("§6[SafeCat] §7Config loaded: §e" + (SafeCatCore.config() != null));
      sender.sendMessage("§6[SafeCat] §7Event bus: §eactive");

      for (var c : currencies) {
        sender.sendMessage(" §8- §7" + c.id() + " §8(" + c.displayName() + ")");
      }
      return CompletableFuture.completedFuture(true);
    }

    if (args[0].equals("currencies")) {
      var currencies = SafeCatAPI.getInstance().getCurrencies();
      if (currencies.isEmpty()) {
        sender.sendMessage("§6[SafeCat] §eNo currencies registered.");
        sender.sendMessage("§7Install a CurrencyProvider mod to add currencies.");
        return CompletableFuture.completedFuture(true);
      }
      sender.sendMessage("§6[SafeCat] §7Registered currencies:");
      for (var c : currencies) {
        sender.sendMessage(
            " §8- §e" + c.id() + " §8(" + c.displayName() + " §7" + c.symbol() + "§8)");
      }
      return CompletableFuture.completedFuture(true);
    }

    sender.sendMessage("§cUnknown subcommand. Use §e/safecat help");
    return CompletableFuture.completedFuture(true);
  }

  private String getVersion() {
    var pkg = getClass().getPackage();
    var ver = pkg != null ? pkg.getImplementationVersion() : null;
    return ver != null ? ver : "1.0-dev";
  }
}
