package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.actions.{BannerRules, VisionRules}
import distantlands.gameplay.powerresolver._
import distantlands.model._

/** Conspiracy: when played, take a relic or a banner from a player whose pawn
  * is at the actor's site, then return the card to the box.
  *
  * The card is played faceup from either origin, a Search's temporary hand or
  * a facedown adviser, and card play places nothing for it, so it is still at
  * its origin when this transform runs. The transform adds, after whatever
  * other powers contributed:
  *  - inside an `AutomaticUse`, which a power-use restriction may refuse the
  *    actor where the walk reaches it (catalog batch 8, slice 10): a
  *    `Decide` offering each legal target, only when there is at least one,
  *    then the take effects for the chosen target;
  *  - the card's removal, outside it, so the card leaves the game even when
  *    nothing was taken or the take was refused.
  *
  * The banner moves by a `Take`, so a restriction on taking sees it; a relic
  * moves by a `Give`. Both are optional. The banner's favor and secrets leave
  * as the `Take`'s children, so a refused `Take` leaves the banner whole.
  *
  * A legal target cannot be declined. The decision id sits under the
  * `cardplay.` prefix, shared with both Search and the facedown-adviser play.
  *
  * The transform must fold to the same vector while the decision is parked,
  * and after the take too: a walk that parks right after it (The Needle) is
  * rebuilt on the state the take left (Legacy in-play powers R20). So the
  * take records itself (`Defer`), and a banner it recorded stays among the
  * offered targets though the actor now holds it.
  *
  * After the take it writes "{Red} seized {relic or banner} from {Blue}.",
  * read from the take's record, and nothing when nothing was taken.
  */
case object ConspiracyWhenPlayed extends ContributingPower:
  val id: PowerId = PowerId("vision.conspiracy")
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  val decisionId: String = "cardplay.conspiracy.target"

  override def applicable(ctx: PowerCtx): Boolean = ctx.operation match
    case CardPlayedFaceup(card, _, _, _) => card == VisionRules.Conspiracy
    case _ => false

  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  private val notes = NoteSet(id)
  /** "{Red} seized {relic or banner} from {Blue}." */
  val seized: NoteKey = notes.key("seized", Vector(NotePart.Arg(0),
    NotePart.Text(" seized "), NotePart.Arg(1), NotePart.Text(" from "),
    NotePart.Arg(2), NotePart.Text(".")))

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.ActionCardPlayedFaceup -> Vector(Transform((ctx, children) =>
      children :+ AutomaticUse(ctx.actor, id, VisionRules.Conspiracy,
        (_, _) => targetDecision(ctx.state, ctx.actor) :+
          BuildOps((ready, pending) =>
            taken(ready, ctx.actor, pending)) :+
          Note(id, seizedNote(ctx.actor)) :+
          BuildOps((ready, _) => Right(
            Option.when(Deferred.held(ready, takeKey(ctx.actor)).nonEmpty)(
              TakeDeferred(takeKey(ctx.actor)): CoreOperation).toVector))) :+
        BuildOps((ready, _) =>
          Right(Vector(removal(ready, ctx.actor)))))))

  /** Where the take records itself, per actor. */
  private def takeKey(actor: PlayerId): DeferralKey =
    DeferralKey(s"conspiracy.take.${actor.value}")

  /** What the recorded take moved to the actor, and from whom, when the
    * actor holds it now: nothing when the take was refused. */
  private def seizedNote(actor: PlayerId)(states: NoteStates)
      : Option[LogNote] =
    val now = states.now
    Deferred.held(now, takeKey(actor)).collectFirst {
      case Give(Piece.Card(relic: RelicId), owner, _, _, _)
          if now.game.current.player(actor)
            .exists(_.relics.exists(_.id == relic)) =>
        (NoteArg.Card(relic), owner)
      case Take(Piece.Banner(banner), _, Location.PlayArea(owner), _, _, _,
          _, _)
          if BannerRules.holder(now.game.current, banner).contains(actor) =>
        (NoteArg.Banner(banner), owner)
    }.map((what, owner) => seized(PowerSourceRef.Card(VisionRules.Conspiracy),
      NoteArg.Player(actor), what, NoteArg.Player(owner)))

  /** The banner the take recorded, if it ran. */
  private def recordedBanner(ready: ReadyGame, actor: PlayerId)
      : Option[DecisionOptionRef] =
    Deferred.held(ready, takeKey(actor)).collectFirst {
      case Take(Piece.Banner(banner), _, _, _, _, _, _, _) =>
        DecisionOptionRef.Banner(banner)
    }

  /** Every relic slot and banner held by another player whose pawn is at the
    * actor's site, in seat order, relic slots before banners.
    */
  private def legalTargets(ready: ReadyGame, actor: PlayerId)
      : Vector[DecisionOptionRef] =
    val current = ready.game.current
    val site = current.pawnSite(actor)
    current.players.filter(other => other.player != actor &&
      other.pawnSite == site).flatMap { other =>
      other.relics.indices.map(slot =>
        DecisionOptionRef.RelicSlot(other.player, slot)) ++
        Banner.all.filter(banner => BannerRules.holder(current, banner)
          .contains(other.player)).map(banner => DecisionOptionRef.Banner(banner))
    }

  /** The legal targets, and a banner the take recorded, which the actor
    * holds now: the question it answered keeps its options. */
  private def offered(ready: ReadyGame, actor: PlayerId)
      : Vector[DecisionOptionRef] =
    val legal = legalTargets(ready, actor)
    legal ++ recordedBanner(ready, actor).filterNot(legal.contains)

  private def targetDecision(ready: ReadyGame, actor: PlayerId)
      : Vector[Operation] =
    val options = offered(ready, actor).flatMap(DecisionOption.forRef)
    if options.isEmpty then Vector.empty
    else Vector(Decide(decisionId, actor, DecisionQuery.ChooseOne(options,
      heading = Some("Conspiracy: choose an enemy asset to take")),
      window = Some(PowerWindow.ConspiracyTargetSelection)))

  private def taken(ready: ReadyGame, actor: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    val legal = legalTargets(ready, actor)
    val chosen = pending.answered.collectFirst:
      case Answered(`decisionId`, DecisionAnswer.ChooseOneAnswer(ref), _) => ref
    chosen match
      case Some(ref) if legal.contains(ref) => Right(
        take(ready, actor, ref).flatMap(op =>
          Vector(Defer(takeKey(actor), Vector(op)), op)))
      case Some(_) => Left(OathViolation.ConspiracyUnavailable(
        "the chosen target is no longer legal"))
      // No decision was asked: no target was legal, or a power at the
      // target window removed every one. The walker never skips a decision
      // that is asked, so nothing is taken.
      case None => Right(Vector.empty)

  private def take(ready: ReadyGame, actor: PlayerId, ref: DecisionOptionRef)
      : Vector[CoreOperation] =
    val current = ready.game.current
    def banner(held: Banner, owner: PlayerId,
        leaving: Vector[CoreOperation]): CoreOperation = Take(
      Piece.Banner(held), actor, Location.PlayArea(owner),
      Location.PlayArea(actor), leaving = leaving)
    ref match
      case DecisionOptionRef.RelicSlot(owner, slot) =>
        current.player(owner).flatMap(_.relics.lift(slot))
          .toVector.map(relic => Give(Piece.Card(relic.id), owner,
            Location.PlayArea(owner), Location.PlayArea(actor)))
      case DecisionOptionRef.Banner(held) =>
        BannerRules.holder(current, held).toVector.flatMap { owner =>
          val leaving: Vector[CoreOperation] = held match
            case Banner.PeoplesFavor => BannerRules.peoplesFavorRibbon(ready)
            case Banner.DarkestSecret =>
              BannerRules.darkestSecretRibbon(ready, actor)
          Vector(banner(held, owner, leaving))
        }
      case _ => Vector.empty

  /** The card leaves the game from where it is played from: the actor's
    * temporary hand, a discard pile (Search Party, catalog batch 10, slice
    * 12) or the actor's play area. */
  private def removal(ready: ReadyGame, actor: PlayerId): CoreOperation =
    val current = ready.game.current
    val conspiracy = VisionRules.Conspiracy
    val from =
      if current.temporaryHands.getOrElse(actor, Vector.empty)
          .contains(conspiracy) then Location.Hand(actor)
      else Region.all.find(current.commonCards.discard(_).contains(conspiracy))
        .fold(Location.PlayArea(actor))(Location.RegionalDiscard(_))
    Move(Piece.Card(conspiracy), PositionedLocation(from),
      PositionedLocation(Location.SharedBank))
