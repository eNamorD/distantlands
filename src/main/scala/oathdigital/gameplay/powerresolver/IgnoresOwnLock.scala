package oathdigital.gameplay.powerresolver

import oathdigital.model.{CardId, PowerId}

/** A phase power whose use may move or flip its own lock-icon card (catalog
  * batch 10, slice 4). While the walker runs the power's use
  * (`ActionRef.UsePower`), the lock on `ownCard` does not bind. Every other
  * card's lock still binds, and `ownCard`'s lock binds every other power.
  * The rulebook words such exemptions the same way: "Bury ignores locked".
  *
  * Operation restrictions run only at command time; replay applies the
  * recorded operations without them. The exemption is therefore never
  * journaled.
  */
trait IgnoresOwnLock extends PhasePower:
  /** The card whose lock this power's use ignores. */
  def ownCard: CardId

object IgnoresOwnLock:
  /** The card each of `powers` may move despite its lock, by power. */
  def exemptions(powers: Vector[PhasePower]): Map[PowerId, CardId] =
    powers.collect { case power: IgnoresOwnLock =>
      power.id -> power.ownCard }.toMap
