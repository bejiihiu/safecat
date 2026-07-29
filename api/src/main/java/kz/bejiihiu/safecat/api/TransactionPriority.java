package kz.bejiihiu.safecat.api;

/**
 * Defines the execution priority for a transaction. Higher-priority transactions are processed
 * first within a given tick.
 *
 * @deprecated unused — the current architecture uses a provider-chain pattern where priority is
 *     expressed via {@code int} values directly on each provider interface. Kept for reference
 *     only; will be removed in a future release.
 */
@Deprecated
public enum TransactionPriority {

  /** Processed before normal transactions. */
  EARLIEST,

  /** Default priority. Most transactions should use this. */
  NORMAL,

  /** Processed after normal transactions. */
  LATE
}
