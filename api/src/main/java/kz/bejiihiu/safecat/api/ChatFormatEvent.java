package kz.bejiihiu.safecat.api;

import java.util.Objects;
import java.util.UUID;

/**
 * A cancelable event fired when a chat message is being formatted. Modify the message or format
 * string to change how the message appears.
 */
public class ChatFormatEvent extends Event {

  private final UUID player;
  private String message;
  private String format;

  /**
   * Creates a new chat format event.
   *
   * @param player the player sending the message
   * @param message the raw message content
   * @param format the format string (e.g. "{prefix} {player}: {message}")
   */
  public ChatFormatEvent(UUID player, String message, String format) {
    this.player = Objects.requireNonNull(player, "player");
    this.message = Objects.requireNonNull(message, "message");
    this.format = Objects.requireNonNull(format, "format");
  }

  /**
   * Returns the player UUID.
   *
   * @return the player UUID
   */
  public UUID getPlayer() {
    return player;
  }

  /**
   * Returns the current message content.
   *
   * @return the current message content
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message content.
   *
   * @param message the new message content
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Returns the current format string.
   *
   * @return the current format string
   */
  public String getFormat() {
    return format;
  }

  /**
   * Sets the format string.
   *
   * @param format the new format string
   */
  public void setFormat(String format) {
    this.format = format;
  }
}
