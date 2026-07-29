package kz.bejiihiu.safecat.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/** A {@link CommandProvider} backed by a lambda, for quick inline command definitions. */
public final class SimpleCommand implements CommandProvider {

  private final String name;
  private final String description;
  private final String permission;
  private final BiFunction<CommandSender, String[], CompletableFuture<Boolean>> executor;

  /**
   * Creates a simple command with full configuration.
   *
   * @param name the command name
   * @param description a short description
   * @param permission the required permission, or null
   * @param executor the execution logic
   */
  public SimpleCommand(
      String name,
      String description,
      String permission,
      BiFunction<CommandSender, String[], CompletableFuture<Boolean>> executor) {
    this.name = name;
    this.description = description;
    this.permission = permission;
    this.executor = executor;
  }

  /**
   * Shorthand that omits description and permission.
   *
   * @param name the command name
   * @param executor the execution logic
   */
  public SimpleCommand(
      String name, BiFunction<CommandSender, String[], CompletableFuture<Boolean>> executor) {
    this(name, "", null, executor);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getPermission() {
    return permission;
  }

  @Override
  public CompletableFuture<Boolean> execute(CommandSender sender, String[] args) {
    return executor.apply(sender, args);
  }
}
