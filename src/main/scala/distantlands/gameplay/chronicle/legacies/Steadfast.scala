package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model.{Cost, LegacyId, PowerId, PowerWindow,
  RuleSourceRef, Suit}

object SteadfastCard extends Legacy(LegacyId("L03"), "Steadfast",
    LegacyGoal.MostSites, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.steadfast"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE:** Your other legacies **cannot** become " +
      "dormant—they always stay active.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Steadfast (legacy L03): "Your other legacies cannot become dormant—they
  * always stay active." Only Sun step 3 makes a legacy dormant, and it asks
  * `ChronicleLegacyRules.keepsOthersActive` at its start (Legacy Chronicle
  * powers design, ruling 5). Steadfast itself can go dormant, and the cap
  * still discards, as a discard is not a dormancy. This power hooks no
  * window. */
case object Steadfast extends ContributingPower:
  val id: PowerId = SteadfastCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
