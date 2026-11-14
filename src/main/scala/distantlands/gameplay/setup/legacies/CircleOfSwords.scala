package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object CircleOfSwordsCard extends Legacy(LegacyId("L06"), "Circle of Swords",
    LegacyGoal.MostSites, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.circle-of-swords"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** You gain eight starting warbands _(instead of " +
      "three)_. If you're Imperial and other Imperials would gain " +
      "fewer than three, you gain less.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Circle of Swords (legacy L06), its SETUP power (Legacy Setup powers
  * design, ruling 5). Game start reads it (`StartingLegacies.of`): the
  * holder's board starts with 8 warbands, an Imperial's from the purple
  * supply after every other Imperial board's 3 (R3). This power hooks no
  * window; it is registered so the card counts as implemented. */
case object CircleOfSwords extends ContributingPower:
  val id: PowerId = CircleOfSwordsCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
  val warbands: Int = 8
