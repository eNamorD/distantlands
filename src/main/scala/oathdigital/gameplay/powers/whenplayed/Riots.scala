package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.model._

object RiotsCard extends Denizen(DenizenId("91"), "Riots", Suit.Discord):
  val power = PrintedPower(PowerId("denizen.riots"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** discard all denizens at sites in this " +
      "region. Burn the same number of [favor] from the People's " +
      "Favor.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Riots (card 91), WHEN PLAYED: discard all denizens at sites in this
  * region. Burn the same number of favor from the People's Favor.
  *
  * The discard is [[RegionDiscard]]'s with every suit kept: every site
  * denizen and ruined edifice in the actor's region, as far as the generic
  * discard rules permit. Played to a site, Riots stands there, so it
  * discards and counts itself. The burn is one favor for each target no
  * site holds after the discard, from the People's Favor, whoever holds it,
  * as far as it holds favor. With nothing discarded nothing is burned, and
  * only the `none` line is written.
  */
case object Riots extends WhenPlayedPower:
  val id: PowerId = RiotsCard.power.id
  private val notes = NoteSet(id)
  notes.include(RegionDiscard.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Burned {n favor} from the {People's Favor}." */
  val burned: NoteKey = notes.key("burned", Vector(NotePart.Text("Burned "),
    NotePart.Arg(0), NotePart.Text(" from the "), NotePart.Arg(1),
    NotePart.Text(".")))
  /** "The {People's Favor} had no favor to burn." */
  val unburned: NoteKey = notes.key("unburned", Vector(NotePart.Text("The "),
    NotePart.Arg(0), NotePart.Text(" had no favor to burn.")))

  private def favorOn(ready: ReadyGame): Int =
    ready.game.current.banners.peoplesFavor.favor

  /** One favor for each of `found` that no site holds now. */
  private def burn(ready: ReadyGame, found: Vector[CardId], actor: PlayerId)
      : Vector[CoreOperation] =
    val count = RegionDiscard.gone(found, ready).size
    if count == 0 then Vector.empty
    else Vector(Burn.favor(count,
      PositionedLocation(Location.OnBanner(Banner.PeoplesFavor)),
      Some(actor)))

  val cardId: DenizenId = RiotsCard.id

  private val region = new RegionDiscard(_ => true)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, _) =>
      val found = region.targets(ctx.catalog, live, ctx.actor)
      Vector(
        BuildOps((ready, _) => region.discards(ctx.catalog, ready, ctx.actor)),
        Note(id, RegionDiscard.said(cardId, found), covers = true),
        BuildOps((ready, _) => Right(burn(ready, found, ctx.actor))),
        Note(id, burnedNote(_, found)))))

  /** What the burn step took from the banner, or that it held none. Nothing
    * when nothing was discarded. */
  private def burnedNote(states: NoteStates, found: Vector[CardId])
      : Option[LogNote] =
    val source = PowerSourceRef.Card(cardId)
    val banner = NoteArg.Banner(Banner.PeoplesFavor)
    val burnt = states.previous.fold(0)((before, after) =>
      favorOn(before) - favorOn(after))
    Option.when(RegionDiscard.gone(found, states.now).nonEmpty)(
      if burnt > 0 then burned(source, NoteArg.Amount(burnt, NoteUnit.Favor),
        banner)
      else unburned(source, banner))
