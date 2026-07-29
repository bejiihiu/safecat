package kz.bejiihiu.safecat.api;

/**
 * Abstract base class for all SafeCat events. Events are cancelable POJOs that can be posted
 * through the SafeCatEventBus.
 */
public abstract class Event {

  /** Creates a new Event with cancelled state set to false. */
  public Event() {}

  private boolean cancelled;

  /**
   * Returns whether this event has been cancelled.
   *
   * @return true if the event is cancelled
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Sets the cancelled state of this event.
   *
   * @param cancelled the new cancelled state
   */
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
