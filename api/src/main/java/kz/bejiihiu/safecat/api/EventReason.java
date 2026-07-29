package kz.bejiihiu.safecat.api;

/** Represents the reason or cause of a balance change or transaction. */
public enum EventReason {

  /** Transaction from a shop or marketplace purchase. */
  SHOP_PURCHASE,

  /** Reward from completing a quest. */
  QUEST_REWARD,

  /** Manual adjustment by an administrator. */
  ADMIN,

  /** Tax collection or fee. */
  TAX,

  /** Interest accrual on a balance. */
  INTEREST,

  /** Player-to-player transfer. */
  TRANSFER
}
