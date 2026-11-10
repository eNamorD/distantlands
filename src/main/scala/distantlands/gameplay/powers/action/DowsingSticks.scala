package distantlands.gameplay.powers.action

import distantlands.catalog.{ExecutableCatalog, PrintedPower, Relic}
import distantlands.gameplay.powers.RelicDraws
import distantlands.model._

object DowsingSticksCard extends Relic(RelicId("R09"), "Dowsing Sticks", value = 9, defense = 1):
  val power = PrintedPower(PowerId("relic.dowsing-sticks"),
    persistent = false, cost = Cost(secret = 1, secretBurnt = 2),
    text = "**ACTION:** Draw a relic from the relic deck and take it. " +
      "_(You may keep it facedown.)_")
  val powers: Vector[PrintedPower] = Vector(power)

/** Dowsing Sticks (relic R09), ACTION: place 1 secret on this relic and burn
  * 2 secrets, then draw a relic and take it facedown. An empty relic deck
  * pays the cost and does nothing else. The draw and its line are
  * `RelicDraws.drawSteps`, shared with Tinker's Fair.
  */
case object DowsingSticks extends PaidAction(DowsingSticksCard.power):
  /** Its line when the deck had no relic to draw. */
  private val notes = NoteSet(id)
  notes.include(RelicDraws.notes)
  val emptyDeck: NoteKey = RelicDraws.emptyDeck
  override def noteKeys: Vector[NoteKey] = notes.declared

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] =
    Right(Sequence(RelicDraws.drawSteps(id, player, source)))
