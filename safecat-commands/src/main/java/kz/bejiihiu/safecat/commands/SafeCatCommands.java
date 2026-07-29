package kz.bejiihiu.safecat.commands;

import java.util.concurrent.CompletableFuture;
import kz.bejiihiu.safecat.api.CommandProvider;
import kz.bejiihiu.safecat.api.CommandSender;
import kz.bejiihiu.safecat.api.Currency;
import kz.bejiihiu.safecat.api.SafeCatAPI;

/** Built-in {@code /safecat} command — help, status, currencies. */
public class SafeCatCommands implements CommandProvider {

  static final String PERM_HELP = "safecat.command.help";
  static final String PERM_CURRENCIES = "safecat.command.currencies";

  private static final String PREFIX = "§6[SafeCat]§r ";

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
    return null;
  }

  @Override
  public CompletableFuture<Boolean> execute(CommandSender sender, String[] args) {
    try {
      if (args.length == 0 || args[0].equals("help")) {
        handleHelp(sender);
      } else if (args[0].equals("status")) {
        handleStatus(sender);
      } else if (args[0].equals("currencies")) {
        handleCurrencies(sender);
      } else {
        sender.sendMessage("§cUnknown subcommand. Use §e/safecat help");
      }
    } catch (Exception e) {
      sender.sendMessage("§cError: " + e.getMessage());
    }
    return CompletableFuture.completedFuture(true);
  }

  private void handleHelp(CommandSender sender) {
    var api = SafeCatAPI.getInstance();
    var formatted = api.format(PREFIX + "§eSafeCat commands:", sender.getUniqueId());
    sender.sendMessage(formatted);
    sender.sendMessage(" §7/safecat help §8— §7show this help");
    sender.sendMessage(" §7/safecat status §8— §7show API status");
    sender.sendMessage(" §7/safecat currencies §8— §7list registered currencies");
  }

  private void handleStatus(CommandSender sender) {
    var api = SafeCatAPI.getInstance();
    var currencies = api.getCurrencies();
    sender.sendMessage(PREFIX + "§7Currencies: §e" + currencies.size());
    sender.sendMessage(PREFIX + "§7API: §aavailable");
    for (var c : currencies) {
      sender.sendMessage(" §8- §7" + c.id() + " §8(" + c.displayName() + ")");
    }
  }

  private void handleCurrencies(CommandSender sender) {
    if (!sender.hasPermission(PERM_CURRENCIES)) {
      sender.sendMessage("§cYou don't have permission to list currencies.");
      return;
    }
    var currencies = SafeCatAPI.getInstance().getCurrencies();
    if (currencies.isEmpty()) {
      sender.sendMessage(PREFIX + "§eNo currencies registered.");
      sender.sendMessage(" §7Install a CurrencyProvider mod to add currencies.");
      return;
    }
    sender.sendMessage(PREFIX + "§7Registered currencies §8(" + currencies.size() + "§8):");
    for (Currency c : currencies) {
      sender.sendMessage(
          " §8- §e"
              + c.id()
              + " §8("
              + c.displayName()
              + " §7"
              + c.symbol()
              + "§8)"
              + " §7["
              + c.type()
              + "]");
    }
  }
}
