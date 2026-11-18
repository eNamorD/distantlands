package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, RelicKeep}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object HeirloomCard extends Legacy(LegacyId("L20"), "Heirloom",
    LegacyGoal.DarkestSecret, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.heirloom"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (THRONE):** Before Scatter Relics, secretly " +
      "choose one of your relics to keep facedown. You will start " +
      "the next game with it.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Heirloom (legacy L20): "Before Scatter Relics, secretly choose one of
  * your relics to keep facedown. You will start the next game with it."
  * Its step is the window before Scatter Relics (Legacy Chronicle powers
  * design, ruling 13), after Stock Reliquary, so a Throne holder's relics
  * are already in the Reliquary (R17). The holder, read when the step is
  * reached, chooses one of their relics but the Grand Scepter, with no
  * Decline; a single one is kept without a question (R18). `RelicKeep`
  * keeps it and writes its lines (R19, R20). */
case object Heirloom extends ContributingPower:
  val id: PowerId = HeirloomCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleBeforeScatterRelics -> Vector(Transform(
      (_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.heirloom.${player.value}"

  val step: Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, HeirloomCard.id).toVector.flatMap(
      holder => RelicKeep.keepable(state, holder) match
        case Vector() => Vector.empty
        case Vector(relic) => keep(holder, relic.id)
        case relics => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Heirloom: keep a relic for the next game"),
            confirmVerb = Some("Keep"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(holder))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(keep(holder, _))))))

  private def keep(holder: PlayerId, relic: RelicId): Vector[Operation] =
    RelicKeep.keep(holder, relic, HeirloomCard.id)
