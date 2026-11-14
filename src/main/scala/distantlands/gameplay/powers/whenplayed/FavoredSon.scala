package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.gameplay.actions.BannerRules
import distantlands.gameplay.powerresolver.{Deferred, PowerCtx}
import distantlands.model._

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
  * walks (`BannerRules.peoplesFavorRibbon`), so the favor returns to the
  * banks, and on the Grand Council half of it to the previous holder, only
  * if the take happens. It is not walked under the `ChallengeRibbon`
  * window, so Secret Testament, a Challenge modifier,
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
    award(ctx.actor)

  /** Where the take records itself for its line, per user. */
  private def takeKey(actor: PlayerId): DeferralKey =
    DeferralKey(s"favored-son.take.${actor.value}")

  /** The take, its line and the record's clearing. The tree has one shape
    * whoever holds the banner: the take reads the holder when it runs, takes
    * nothing from the user, and records itself (`Defer`) for the line, so a
    * walk that parks after the take (The Needle) and is rebuilt on the
    * state the take left keeps its line (Legacy in-play powers R20). */
  private def award(actor: PlayerId): Vector[Operation] = Vector(
    BuildOps((live, _) => Right(
      BannerRules.holder(live.game.current, Banner.PeoplesFavor) match
        case Some(holder) if holder == actor => Vector.empty
        case holder =>
          val take = Take(Piece.Banner(Banner.PeoplesFavor), actor,
            holder.fold[Location](Location.SharedBank)(Location.PlayArea(_)),
            Location.PlayArea(actor),
            leaving = BannerRules.peoplesFavorRibbon(live))
          Vector(Defer(takeKey(actor), Vector(take)), take))),
    Note(id, states =>
      if Deferred.held(states.now, takeKey(actor)).isEmpty then
        Some(held(cardSource, banner))
      else takenNote(states, actor)),
    BuildOps((live, _) => Right(
      Option.when(Deferred.held(live, takeKey(actor)).nonEmpty)(
        TakeDeferred(takeKey(actor)): CoreOperation).toVector)))

  /** Whether the user holds the banner now, or Lost Tongue kept it. */
  private def takenNote(states: NoteStates, actor: PlayerId)
      : Option[LogNote] = Some(
    if BannerRules.holder(states.now.game.current, Banner.PeoplesFavor)
        .contains(actor)
    then took(cardSource, banner)
    else none(cardSource, banner))
