package kz.bejiihiu.safecat.api;

import java.util.concurrent.CompletableFuture;

/** A single SafeCat command. */
public interface CommandProvider {

  /**
   * Returns the command name (e.g. "balance").
   *
   * @return the command name
   */
  String getName();

  /**
   * Returns a short description of what the command does.
   *
   * @return the command description
   */
  String getDescription();

  /**
   * Returns the permission node required to use the command, or null.
   *
   * @return the permission node, or null
   */
  String getPermission();

  /**
   * Executes the command.
   *
   * @param sender who ran the command
   * @param args the arguments (excluding the command name)
   * @return a future completing with true on success
   */
  CompletableFuture<Boolean> execute(CommandSender sender, String[] args);
}
