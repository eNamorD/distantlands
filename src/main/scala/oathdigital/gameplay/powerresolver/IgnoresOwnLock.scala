package oathdigital.gameplay.powerresolver

import oathdigital.model.{CardId, PowerId}

/** A phase power whose use may move or flip its own lock-icon card (catalog
  * batch 10, slice 4). While the walker runs the power's use, the lock on
  * `ownCard` does not bind. The walk is the unit: an operation another
  * power adds inside that walk runs unlocked too. Every other card's lock
  * still binds, and `ownCard`'s lock binds every other walk. The rulebook
  * words such exemptions the same way: "Bury ignores locked".
  *
  * Only a use walked as `ActionRef.UsePower`, which a phase menu starts,
  * is exempt. A forced Wake power runs under `ForcedWake` and would not be.
  *
  * Operation restrictions run only at command time. Replay applies the
  * recorded operations without them, so the exemption is never journaled.
  */
trait IgnoresOwnLock extends PhasePower:
  /** The card whose lock this power's use ignores. */
  def ownCard: CardId

object IgnoresOwnLock:
  /** The card each of `powers` may move despite its lock, by power. */
  def exemptions(powers: Vector[PhasePower]): Map[PowerId, CardId] =
    powers.collect { case power: IgnoresOwnLock =>
      power.id -> power.ownCard }.toMap
