package distantlands.gameplay.chronicle

import distantlands.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object StoneBathsCard extends Edifice(EdificeId("E04"), Suit.Discord):
  object intact extends EdificeFace("Stone Baths") with Locked:
    val power = PrintedPower(PowerId("edifice.e04.intact"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (SUN): Players with a pawn at this site do not " +
        "discard their dormant legacies.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined Baths"):
    val power = PrintedPower(PowerId("edifice.e04.ruined"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (SUN): Exiles at this site don't need to meet the " +
        "goals of their active legacies to keep them active.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Who the Baths exempt (catalog batch 13, rulings 14 to 16). The Sun is
  * the Chronicle's first task, so the edifice and the pawns are still on
  * the map when Resolve Legacies reads them, and a pawn off the map is at
  * no site. */
object BathsRules:
  /** The seated players whose pawns stand at the site where E04 shows
    * `side`: anyone at Stone Baths (Sun step 2), only Exiles at Ruined
    * Baths (Sun step 3). Nobody when that face is not on the map. */
  def exempt(ready: ReadyGame, side: EdificeSide): Set[PlayerId] =
    val current = ready.game.current
    current.edificeSite(StoneBathsCard.id, side).fold(Set.empty)(site =>
      current.players.collect {
        case player if player.pawnSite.contains(site) &&
            (side == EdificeSide.Intact ||
              Imperials.role(ready, player.player).contains(Role.Exile)) =>
          player.player
      }.toSet)

/** Stone Baths (E04 intact): Sun step 2 reads it
  * (`ResolveLegacies.flip`). It hooks no window. */
case object StoneBaths extends ContributingPower:
  val id: PowerId = StoneBathsCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Ruined Baths (E04 ruined): Sun step 3 reads it
  * (`ResolveLegacies.dormancy`). It hooks no window. */
case object RuinedBaths extends ContributingPower:
  val id: PowerId = StoneBathsCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
