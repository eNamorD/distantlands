package distantlands.testkit

import distantlands.gameplay.powers.LegacyDraws
import distantlands.model.{DecisionAnswer, DecisionOptionRef, DecisionPlacement,
  DecisionQuery, LegacyId}

/** Answers to a parked decision, built the way a client would send them. */
object DecisionAnswers:
  /** The answer that picks `ref` from a ChooseOne question. */
  def chooseOne(ref: DecisionOptionRef): DecisionAnswer =
    DecisionAnswer.ChooseOneAnswer(ref)

  /** A legacy keep (Setup's or Ancient Writ's): `kept` in Keep, `discarded`
    * in Discard. */
  def keepLegacy(kept: LegacyId, discarded: LegacyId*): DecisionAnswer =
    legacyPick(LegacyDraws.Pick.keep, kept, discarded.toVector)

  /** Amber Doors' activation: `activated` in Activate, `dormant` in
    * Dormant. */
  def activateLegacy(activated: LegacyId, dormant: LegacyId*): DecisionAnswer =
    legacyPick(LegacyDraws.Pick.activate, activated, dormant.toVector)

  /** Lost Doors' discard: `discarded` in Discard, `kept` in Keep. */
  def discardLegacy(discarded: LegacyId, kept: LegacyId*): DecisionAnswer =
    legacyPick(LegacyDraws.Pick.discard, discarded, kept.toVector)

  /** `picked` alone in the one-legacy section of the legacy pick `query`,
    * every other offered legacy in its other section, whichever pick
    * (keep, activate or discard) `query` is. */
  def pickOfferedLegacy(query: DecisionQuery, picked: LegacyId)
      : DecisionAnswer = query match
    case partition: DecisionQuery.Partition =>
      val alone = partition.sections.find(_.maxAllowed.contains(1))
        .getOrElse(sys.error(s"not a legacy pick: $query"))
      val rest = partition.sections.find(_.key != alone.key)
        .getOrElse(sys.error(s"not a legacy pick: $query"))
      DecisionAnswer.PartitionAnswer(partition.options.map(_.ref).map(ref =>
        DecisionPlacement(ref,
          if ref == DecisionOptionRef.Legacy(picked) then alone.key
          else rest.key)))
    case other => sys.error(s"not a legacy pick: $other")

  /** Every legacy a legacy pick's partition offers, in option order. */
  def offeredLegacies(query: DecisionQuery): Vector[LegacyId] = query match
    case partition: DecisionQuery.Partition => partition.options.map(_.ref)
      .collect { case DecisionOptionRef.Legacy(id) => id }
    case _ => Vector.empty

  /** `picked` in `pick`'s one-legacy section, every one of `rest` in the
    * other. */
  private def legacyPick(pick: LegacyDraws.Pick, picked: LegacyId,
      rest: Vector[LegacyId]): DecisionAnswer =
    DecisionAnswer.PartitionAnswer(
      DecisionPlacement(DecisionOptionRef.Legacy(picked), pick.picked.key) +:
        rest.map(id =>
          DecisionPlacement(DecisionOptionRef.Legacy(id), pick.rest.key)))
