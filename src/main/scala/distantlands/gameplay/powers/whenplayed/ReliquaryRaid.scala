package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.ReliquaryDraws
import distantlands.model._

object ReliquaryRaidCard extends Denizen(DenizenId("220"), "Reliquary Raid", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.reliquary-raid"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** take a random relic from the Reliquary.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Reliquary Raid (card 220, adviser-only, locked), WHEN PLAYED: take a
  * random relic from the Reliquary (catalog batch 13, ruling 7).
  *
  * The draw is `ReliquaryDraws`': the Reliquary is shuffled through the
  * walker and its first relic goes facedown to the player's play area. An
  * empty Reliquary, or one Setup With No Empire set aside, takes nothing
  * and writes "The Reliquary was empty." (ruling R22; Foundations ruling
  * 3). Whether there is a relic to take is read when the card resolves, so
  * "Took a relic from the Reliquary." is written only when one was taken.
  * The line never names the relic: it is facedown.
  */
case object ReliquaryRaid extends WhenPlayedPower:
  val id: PowerId = ReliquaryRaidCard.power.id
  val cardId: DenizenId = ReliquaryRaidCard.id
  private val notes = NoteSet(id)
  /** "Took a relic from the Reliquary." */
  val took: NoteKey = notes.key("took",
    Vector(NotePart.Text("Took a relic from the Reliquary.")))
  notes.borrow(ReliquaryDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared

  private def card: PowerSourceRef = PowerSourceRef.Card(cardId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, _) =>
      val stocked = live.game.campaign.reliquaryInPlay.nonEmpty
      ReliquaryDraws.steps(id, ctx.actor, card) ++
        (if stocked then Vector(Note(id, _ => Some(took(card))))
          else Vector.empty)))
