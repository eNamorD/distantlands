package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object ScepterBearerCard extends Legacy(LegacyId("L09"), "Scepter Bearer",
    LegacyGoal.GrandScepter, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.scepter-bearer"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **SETUP:** You start with the " +
      "Grand Scepter _(instead of the Chancellor)_.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Scepter Bearer (legacy L09), its SETUP power (Legacy Setup powers
  * design, ruling 6). An Imperial holder starts with the Grand Scepter in
  * place of the Chancellor (`LaterGameSetup`); the Chancellor keeps the
  * title and the Reliquary pick. With no Empire the Grand Scepter stays in
  * the box. This power hooks no window. */
case object ScepterBearer extends ContributingPower:
  val id: PowerId = ScepterBearerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
