package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.actions.challenge.ChallengeRibbon
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.model._

object FavoredSonCard extends Denizen(DenizenId("235"), "Favored Son", Suit.Hearth) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.favored-son"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** take the People's Favor. If you do, adjust " +
      "its [favor] using its left ribbon.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Favored Son (card 235), WHEN PLAYED: take the People's Favor; if you do,
  * adjust its favor using its left ribbon (catalog batch 10, slice 11).
  *
  * The user takes the banner from its holder or, unheld, from the shared
  * bank, by a `Take` that is not required, as Wizard's Conclave takes the
  * Darkest Secret. Its `leaving` is the People's Favor ribbon a Challenge
  * walks (`ChallengeRibbon.peoplesFavorReturns`), so the favor returns to
  * the banks only if the take happens. It is not walked under the
  * `ChallengeRibbon` window, so Secret Testament, a Challenge modifier,
  * stays out (ruling 11). When the user already holds the banner, nothing
  * moves. When Lost Tongue refuses the take, the banner and its favor stay.
  */
case object FavoredSon extends WhenPlayedPower:
  val id: PowerId = FavoredSonCard.power.id
  val cardId: DenizenId = FavoredSonCard.id
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Took the {People's Favor}." */
  val took: NoteKey = notes.key("took", Vector(NotePart.Text("Took the "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "Already held the {People's Favor}." */
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Already held the "), NotePart.Arg(0), NotePart.Text(".")))
  /** "The {People's Favor} did not move." */
  val none: NoteKey = notes.key("none", Vector(NotePart.Text("The "),
    NotePart.Arg(0), NotePart.Text(" did not move.")))

  private def cardSource: PowerSourceRef = PowerSourceRef.Card(cardId)
  private val banner = NoteArg.Banner(Banner.PeoplesFavor)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, _) => award(live, ctx.actor)))

  /** The take and its line, or the line that the user already held it.
    * The shape depends on the holder, which the take changes, so nothing
    * may park between this Branch and its Note: a resume would re-select
    * the other shape (as for Wizard's Conclave). */
  private def award(ready: ReadyGame, actor: PlayerId): Vector[Operation] =
    val holder = BannerRules.holder(ready.game.current, Banner.PeoplesFavor)
    if holder.contains(actor) then Vector(Note(id, _ =>
      Some(held(cardSource, banner))))
    else Vector(
      BuildOps((live, _) => Right(Vector(Take(
        Piece.Banner(Banner.PeoplesFavor), actor,
        holder.fold[Location](Location.SharedBank)(Location.PlayArea(_)),
        Location.PlayArea(actor),
        leaving = ChallengeRibbon.peoplesFavorReturns(live))))),
      Note(id, takenNote(_, actor)))

  /** Whether the user holds the banner now, or Lost Tongue kept it. */
  private def takenNote(states: NoteStates, actor: PlayerId)
      : Option[LogNote] = Some(
    if BannerRules.holder(states.now.game.current, Banner.PeoplesFavor)
        .contains(actor)
    then took(cardSource, banner)
    else none(cardSource, banner))
