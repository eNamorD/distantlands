package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model.{Cost, LegacyId, PowerId, PowerWindow,
  RuleSourceRef, Suit}

object GreatArchitectCard extends Legacy(LegacyId("L07"), "Great Architect",
    LegacyGoal.GrandScepter, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.great-architect"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **CHRONICLE (THRONE):** You " +
      "perform Build Edifice and may build any edifice _(not just " +
      "a matching one)_. Do this even if the Iron Hand is in play.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Great Architect (legacy L07): "If you are an Imperial: you perform Build
  * Edifice and may build any edifice (not just a matching one)." Build
  * Edifice asks `ChronicleLegacyRules.builder` and `anyEdifice` when each
  * suit's offer is reached (Legacy Chronicle powers design, ruling 10):
  * an Imperial holder performs the step in place of the Throne holder, and
  * any card at a site they rule may be buried for each available edifice.
  * "Even if the Iron Hand is in play": an Imperial holder builds in place
  * of Iron Hand's holder too, who then does not build (ruling 19; R32,
  * re-ruled by the owner 2026-11-14). This power hooks no window. */
case object GreatArchitect extends ContributingPower:
  val id: PowerId = GreatArchitectCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
