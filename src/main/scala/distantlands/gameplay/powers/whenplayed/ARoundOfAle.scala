package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{Denizen, PrintedPower, SiteOnly}
import distantlands.gameplay.phases.RestCleanup
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.model._

object ARoundOfAleCard extends Denizen(DenizenId("129"), "A Round of Ale", Suit.Hearth) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.a-round-of-ale"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** return all [favor] and [secret], and flip " +
      "your [secret], as in the Rest Phase.")
  val powers: Vector[PrintedPower] = Vector(power)

/** A Round of Ale (card 129, site-only), WHEN PLAYED: return all favor and
  * secrets, and flip your secrets, as in the Rest Phase.
  *
  * It runs the Rest cleanup ([[RestCleanup]]) with the actor as the resting
  * player, in one step: favor on every card in play to its suit's bank,
  * secrets on cards to the actor, and the actor's facedown secrets flipped
  * faceup. The step declares no Rest favor window, so League Treaty does not
  * redirect the favor. Cards cleared of tokens may be used again; powers
  * used this turn stay used. Its line comes first, and the cleanup writes
  * no generic line.
  */
case object ARoundOfAle extends WhenPlayedPower:
  val id: PowerId = ARoundOfAleCard.power.id
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Triggering resource cleanup." */
  val cleanup: NoteKey = notes.key("cleanup", Vector(
    NotePart.Text("Triggering resource cleanup.")))

  val cardId: DenizenId = ARoundOfAleCard.id

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(
      Note(id, _ => Some(cleanup(PowerSourceRef.Card(cardId)))),
      BuildOps((ready, _) =>
        RestCleanup.operations(ctx.catalog, ready, ctx.actor)))
