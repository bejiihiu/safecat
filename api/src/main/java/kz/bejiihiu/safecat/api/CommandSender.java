package kz.bejiihiu.safecat.api;

import java.util.UUID;

/** A generic command sender — no Minecraft references. */
public interface CommandSender {

  /**
   * Returns the sender's UUID.
   *
   * @return the sender's UUID
   */
  UUID getUniqueId();

  /**
   * Returns the sender's display name.
   *
   * @return the sender's display name
   */
  String getName();

  /**
   * Checks whether the sender has the given permission.
   *
   * @param permission the permission node to check
   * @return true if the sender has the given permission
   */
  boolean hasPermission(String permission);

  /**
   * Sends a message to the sender.
   *
   * @param message the message to send
   */
  void sendMessage(String message);
}
