package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{Denizen, PrintedPower}
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.model._

object DazzleCard extends Denizen(DenizenId("35"), "Dazzle", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.dazzle"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** discard all [suit-hearth] and [suit-order] " +
      "cards at sites in your region.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Dazzle (card 35), WHEN PLAYED: discard all Hearth and Order cards at
  * sites in your region.
  *
  * The discard is [[RegionDiscard]]'s, with Hearth and Order kept: the
  * Hearth and Order site denizens and ruined edifices in the actor's region,
  * as far as the generic discard rules permit. Its line names the cards it
  * discarded, "Discarded {cards}.", and covers the generic discard lines.
  * With nothing discarded it says "Nothing was discarded."
  */
case object Dazzle extends WhenPlayedPower:
  val id: PowerId = DazzleCard.power.id
  private val suits: Set[Suit] = Set(Suit.Hearth, Suit.Order)

  val cardId: DenizenId = DazzleCard.id

  private val notes = NoteSet(id)
  notes.include(RegionDiscard.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  private val region = new RegionDiscard(id, Dazzle.suits)

  def effect(ctx: PowerCtx): Vector[Operation] =
    region.effect(ctx.catalog, id, cardId, ctx.actor)
