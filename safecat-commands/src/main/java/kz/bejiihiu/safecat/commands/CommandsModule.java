package kz.bejiihiu.safecat.commands;

import kz.bejiihiu.safecat.api.SafeCatAPI;

/**
 * Entry point for the safecat-commands module.
 *
 * <p>Each platform loader calls {@link #register()} during its initialisation, after {@code
 * SafeCatCore.initialize().join()} has completed.
 */
public final class CommandsModule {

  private static volatile boolean registered = false;

  private CommandsModule() {}

  /** Creates and registers all user-facing SafeCat commands. Safe to call multiple times. */
  public static void register() {
    if (registered) return;
    registered = true;

    var api = SafeCatAPI.getInstance();
    api.registerCommand(new SafeCatCommands());
  }
}
