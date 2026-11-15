package distantlands.gameplay.powers.banner

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.operations.PowerUseRules
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  PowerCtx, PowerUse, PowerUseRefusal, PowerUseRestriction, PowerUseStep,
  Transform, Waiver}
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.gameplay.powers.poweruse.Placements
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
  *
  * The marker's persistent power, from the rulebook ("Festival (Darkest
  * Secret)"): "Your enemies cannot place favor or secrets on denizens at
  * the site with the festival marker, and cannot use their powers unless
  * they're persistent powers. They can still peek at and recover relics."
  * Every player but the holder is an enemy. It guards the denizens at the
  * marked site, edifices included, while the Festival face is up and a
  * player holds it: an enemy's use of a guarded card's non-persistent
  * power is refused, as is a persistent use whose cost places on it
  * ([[Placements.byUse]]) and any placement onto one ([[Placements.onto]]),
  * as City Wall's are. Relics, the site's own powers and the cards
  * elsewhere are untouched. The holder is the granter: they may consent to
  * an enemy ignoring it, outside a Campaign, and one grant under the card's
  * use key covers both kinds of refusal. It writes no line of its own.
  */
case object DarkestSecretFestival extends ContributingPower
    with PowerUseRestriction:
  val id: PowerId = PowerId("banner.darkest-secret.festival")
  def source: RuleSourceRef = RuleSourceRef.Banner(Banner.DarkestSecret.key)

  private val notes = NoteSet(id)
  /** "Held the festival at {Deep Woods}". */
  val held: NoteKey = notes.key("held", Vector(
    NotePart.Text("Held the festival at "), NotePart.Arg(0)))
  /** "Cannot use the non-persistent powers of denizens at the festival." */
  val usePlain: NoteKey = notes.key("no-powers-here", Vector(NotePart.Text(
    "Cannot use the non-persistent powers of denizens at the festival.")))
  /** "... without the Darkest Secret holder's consent." */
  val useAsked: NoteKey = notes.key("no-powers-here-consent", Vector(
    NotePart.Text("Cannot use the non-persistent powers of denizens at the " +
      "festival without the Darkest Secret holder's consent.")))
  /** "Cannot place favor or secrets on denizens at the festival." */
  val placingPlain: NoteKey = notes.key("no-placing-here", Vector(
    NotePart.Text("Cannot place favor or secrets on denizens at the " +
      "festival.")))
  /** "... without the Darkest Secret holder's consent." */
  val placingAsked: NoteKey = notes.key("no-placing-here-consent", Vector(
    NotePart.Text("Cannot place favor or secrets on denizens at the " +
      "festival without the Darkest Secret holder's consent.")))
  /** "{card} stays deactivated." (automatic power consent design). */
  val staysDeactivated: NoteKey = notes.borrow(PowerUseStep.staysDeactivated)
  override def noteKeys: Vector[NoteKey] = notes.declared

  private val banner = PowerSourceRef.Banner(Banner.DarkestSecret)

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

  // ---- the marker's persistent power -------------------------------------

  override val waiver: Option[Waiver] =
    Some(Waiver(holder, banner, (_, _) => useAsked(banner)))
  override def powerUseRestrictions: Vector[PowerUseRestriction] =
    Vector(this)
  override def operationRestrictions(catalog: ExecutableCatalog)
      : Vector[OperationRestriction] =
    Vector(placing)

  /** The holder, when `card` is a denizen at the marked site and `player`
    * is their enemy. */
  private def guardedFrom(ready: ReadyGame, player: PlayerId, card: CardId)
      : Option[PlayerId] =
    for
      granter <- holder(ready)
      if granter != player
      site <- ready.game.current.banners.darkestSecret.festival
      state <- ready.game.current.map.sites.get(site)
      if state.denizens.exists(_.id == card)
    yield granter

  /** The plain and asked reasons for `use`: the use's own when it is
    * non-persistent, the placing ones when its cost places now. */
  private def reasons(ready: ReadyGame, use: PowerUse)
      : Option[(NoteKey, NoteKey)] =
    if !use.persistent then Some(usePlain -> useAsked)
    else Placements.byUse(ready, use).map(_ => placingPlain -> placingAsked)

  def refusal(catalog: ExecutableCatalog, ready: ReadyGame, use: PowerUse)
      : Option[PowerUseRefusal] =
    for
      card <- use.card
      granter <- guardedFrom(ready, use.user, card)
      keys <- reasons(ready, use)
    yield PowerUseRefusal(id, keys._1(banner), refused("festival", use.user,
      granter, PowerUseRules.target(use), keys))

  /** The refusal of `player`, waivable by `granter` under the use key of
    * `target`. */
  private def refused(code: String, player: PlayerId, granter: PlayerId,
      target: DecisionOptionRef, keys: (NoteKey, NoteKey)): OperationReason =
    OperationReason(code, s"the festival refuses ${player.value}",
      OperationReasonKind.Impossible, Some(OperationWaiver(id, Some(granter),
        ConsentKey(id, player, PowerUseRules.useKey, target), keys._2(banner),
        keys._1(banner))))

  private object placing extends OperationRestriction:
    def reason(ready: ReadyGame, operation: CoreOperation)
        : Option[OperationReason] =
      Placements.onto(ready, operation).flatMap((player, card) =>
        guardedFrom(ready, player, card).map(granter => refused(
          "festival-placing", player, granter, PowerUseRules.cardRef(card),
          placingPlain -> placingAsked)))
