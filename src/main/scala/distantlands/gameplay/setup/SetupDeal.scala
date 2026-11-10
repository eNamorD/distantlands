package distantlands.gameplay.setup

import distantlands.model._

/** The deal's arithmetic that the plan factory and game start share. */
object SetupDeal:
  /** Players who draw 3 denizens in Foundation IV: those whose lineage has
    * no starting advisers (CR p. 11). */
  def drawers(chronicle: Chronicle,
      participants: Vector[FirstGameParticipant]): Int =
    val advised = chronicle.lineages.filter(_.startingAdvisers.nonEmpty)
      .map(_.id).toSet
    participants.count(p => !advised(p.lineageId))

  /** The world cards dealt before the deck top: 2 seeded discards in each
    * region and 3 for each drawer's hand. */
  def dealt(drawers: Int): Int = 6 + drawers * 3
