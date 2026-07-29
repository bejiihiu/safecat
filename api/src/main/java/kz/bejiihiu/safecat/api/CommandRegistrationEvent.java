package kz.bejiihiu.safecat.api;

/** Fired when the SafeCat registry is ready for command registrations. */
public class CommandRegistrationEvent extends Event {

  private final SafeCatRegistry registry;

  /**
   * Creates a new command registration event.
   *
   * @param registry the registry commands can be registered with
   */
  public CommandRegistrationEvent(SafeCatRegistry registry) {
    this.registry = registry;
  }

  /**
   * Returns the registry.
   *
   * @return the registry
   */
  public SafeCatRegistry getRegistry() {
    return registry;
  }
}
