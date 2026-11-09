package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.model._

object ThreateningRoarCard extends Denizen(DenizenId("179"), "Threatening Roar", Suit.Beast):
  val power = PrintedPower(PowerId("denizen.threatening-roar"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** discard all [suit-nomad] and [suit-beast] " +
      "cards at sites in your region.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Threatening Roar (card 179), WHEN PLAYED: discard all Nomad and Beast
  * cards at sites in your region.
  *
  * The discard is [[RegionDiscard]]'s, as Dazzle's is, with Nomad and Beast
  * kept. Played to a site, it is a Beast card at a site in the region, so it
  * discards itself.
  */
case object ThreateningRoar extends WhenPlayedPower:
  val id: PowerId = ThreateningRoarCard.power.id
  private val suits: Set[Suit] = Set(Suit.Nomad, Suit.Beast)

  val cardId: DenizenId = ThreateningRoarCard.id

  private val notes = NoteSet(id)
  notes.include(RegionDiscard.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  private val region = new RegionDiscard(id, ThreateningRoar.suits)

  def effect(ctx: PowerCtx): Vector[Operation] =
    region.effect(ctx.catalog, id, cardId, ctx.actor)
