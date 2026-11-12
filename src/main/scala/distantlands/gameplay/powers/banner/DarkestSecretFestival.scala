package distantlands.gameplay.powers.banner

import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, Transform}
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.model._

/** Festival (the Banner of the Darkest Secret, Festival face): "REST: You
  * must place the festival marker over the cards at your site."
  * (Foundations design, slice 6).
  *
  * A rule of the banner's holder, automatic, at the `RestStart` window that
  * Begin Rest folds: it asks nothing, since the face says "must". The
  * marker goes to the site the holder's pawn stands at, by a journaled
  * `SetFestivalMarker`, wherever it lay before; a holder with no pawn on
  * the map places nothing and the marker stays. The Rest of a player who
  * does not hold the face, or of the holder of the Wandering Flame, places
  * nothing.
  *
  * Its line names the site and not the holder, whose turn it is: "Held the
  * festival at Deep Woods".
  */
case object DarkestSecretFestival extends ContributingPower:
  val id: PowerId = PowerId("banner.darkest-secret.festival")
  def source: RuleSourceRef = RuleSourceRef.Banner(Banner.DarkestSecret.key)

  private val notes = NoteSet(id)
  /** "Held the festival at {Deep Woods}". */
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Held the festival at "), NotePart.Arg(0)))
  override def noteKeys: Vector[NoteKey] = notes.declared

  /** The player holding the Darkest Secret while its Festival face is up. */
  def holder(ready: ReadyGame): Option[PlayerId] =
    val secret = ready.game.current.banners.darkestSecret
    secret.holder.filter(_ => secret.active == DarkestSecretFace.Festival)

  /** The resting player holds the Festival. */
  override def applicable(ctx: PowerCtx): Boolean =
    holder(ctx.state).contains(ctx.actor)

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.RestStart -> Vector(Transform((ctx, ops) => ops ++ Vector(
      BuildOps((ready, _) => Right(placed(ready, ctx.actor))),
      Note(id, line(_, ctx.actor))))))

  /** The marker at `rester`'s pawn site, or nothing with no pawn there. */
  private def placed(ready: ReadyGame, rester: PlayerId)
      : Vector[CoreOperation] =
    PawnMoves.pawnSite(ready, rester).toOption.toVector.map(site =>
      SetFestivalMarker(Some(site)))

  /** The line, once the marker lies at `rester`'s pawn site. */
  private def line(states: NoteStates, rester: PlayerId): Option[LogNote] =
    PawnMoves.pawnSite(states.now, rester).toOption
      .filter(site => states.now.game.current.banners.darkestSecret.festival
        .contains(site))
      .map(site => held.sourceless(NoteArg.Site(site)))
