package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.{RevealedVisions, TurnOrder}
import oathdigital.gameplay.actions.PlacementLimit
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powerresolver.{Contribution, ExecutedBatch,
  ExecutedTrigger, Transform}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** False Prophet's claim, when its Vision is discarded (card 85; catalog
  * batch 11, rulings 14, 32 and 33): "If it is ever discarded, kill the
  * warband and play or discard the Vision as if you had searched."
  *
  * After a batch that discards a Vision carrying a claimant's warband, the
  * follow-up, for each such Vision in the batch's order:
  *
  *  1. asks the current player which claimant plays it, when there are
  *     several (rule 15.3; ruling 32);
  *  2. nests the claimant's card play of the Vision from the pile it went
  *     to, made by this power, as Search Party's play is; `claimRule`
  *     leaves it faceup or discard (ruling 33), and the hook names the
  *     claimant, so the Vision rules judge the play for them;
  *  3. kills every claimant's warband on it, each with its line.
  *
  * A command that resumes inside the follow-up asks again, with the held
  * operations and the parked state, and must get the same tree
  * (`ExecutedTrigger`). The claimants are read from the warbands on the
  * Vision, so the kill comes last; and the pile from the executed discard,
  * since the claimant's play moves the card.
  */
case object FalseProphetClaim extends ExecutedTrigger:
  val cardId: DenizenId = FalseProphetCard.id
  /** Its own id beside the When Played's printed one, since the walker
    * catalog holds one power per id. Its lines and its card play name it. */
  val id: PowerId = PowerId(s"${FalseProphetCard.power.id.value}.claim")
  val toFaceupOrDiscard: String =
    "False Prophet: reveal the Vision or discard it"

  def claimantDecisionId(vision: VisionId): String =
    s"power.false-prophet.claimant.${vision.value}"

  /** Only the faceup play, or the discard, stays open (ruling 33). */
  val claimRule: PlacementLimit = PlacementLimit(
    OptionReason.Written(toFaceupOrDiscard), {
      case SearchPlacement.Adviser(Orientation.FaceDown, _) => false
      case _ => true
    })

  private val notes = NoteSet(id)
  /** "Killed {Blue}'s warband on {Vision of Faith}." */
  val killed: NoteKey = notes.key("killed", Vector(
    NotePart.Text("Killed "), NotePart.Arg(0),
    NotePart.Text("'s warband on "), NotePart.Arg(1), NotePart.Text(".")))
  /** "Killed the warband on {Vision of Faith}.", the walk's actor's. */
  val killedOwn: NoteKey = notes.key("killed.own", Vector(
    NotePart.Text("Killed the warband on "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  /** This power's own play goes faceup or to the discard. */
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match
        case tree: CardPlayProcedure.PlacementTree
            if tree.byPower.contains(id) =>
          tree.adjust(children)(_.limitPlacements(claimRule))
        case _ => children)))

  def onExecuted(batch: ExecutedBatch): Option[Operation] =
    val claims = discarded(batch.executed).filter((vision, _) =>
      RevealedVisions.claimants(batch.now, vision).nonEmpty)
    Option.when(claims.nonEmpty)(Sequence(claims.map((vision, pile) =>
      Sequence(follow(batch, vision, pile)))))

  /** Each Vision the batch discarded, with the pile it went to, at any
    * depth of the batch's operations. */
  private def discarded(executed: Vector[CoreOperation])
      : Vector[(VisionId, Region)] =
    def all(operation: Operation): Vector[Operation] = operation match
      case _: LeafOperation => Vector(operation)
      case _ => operation +: operation.children.flatMap(all)
    executed.flatMap(all).collect {
      case discard: Discard.Vision => (discard.card, discard.to) }.distinct

  private def follow(batch: ExecutedBatch, vision: VisionId, pile: Region)
      : Vector[Operation] =
    val claimants = RevealedVisions.claimants(batch.now, vision)
    val current = batch.now.game.current.turn.activePlayer
    val chooserId = claimantDecisionId(vision)
    // In turn order from the current player, who chooses (rule 15.3).
    val ordered = rotated(TurnOrder.order(batch.now), current)
      .filter(claimants.contains)
    val choose = Option.when(ordered.size > 1)(Decide(chooserId, current,
      DecisionQuery.ChooseOne(ordered.map(player =>
        DecisionOption.Player(DecisionOptionRef.Player(player))),
        heading = Some("False Prophet: choose who plays the Vision"))))
      .toVector
    def player(pending: PendingTree): Option[PlayerId] =
      if ordered.size == 1 then ordered.headOption
      else PowerAnswers.one(pending, chooserId).collect {
        case DecisionOptionRef.Player(chosen) => chosen }
    val play = Branch((now, pending) => player(pending).toVector.map(
      claimant => CardPlayProcedure.unchecked(batch.catalog, now, claimant,
        vision, CardPlayProcedure.Origin.DiscardPile(pile),
        byPower = Some(id))))
    val kills = claimants.flatMap(claimant => Vector[Operation](
      BuildOps((live, _) => Right(live.game.current.player(claimant).toVector
        .flatMap { state =>
          val kind = ForceKind.Exile(state.lineage)
          val count = live.game.current.warbandsOn(vision, kind)
          Option.when(count > 0)(Kill(Piece.Warbands(kind, count),
            PositionedLocation(Location.OnCard(vision)))).toVector
        })),
      Note(id, killedNote(claimant, vision), covers = true)))
    choose ++ Vector(play) ++ kills

  private def rotated(players: Vector[PlayerId], first: PlayerId)
      : Vector[PlayerId] =
    val index = math.max(0, players.indexOf(first))
    players.drop(index) ++ players.take(index)

  private def killedNote(claimant: PlayerId, vision: VisionId)(
      states: NoteStates): Option[LogNote] =
    val card = PowerSourceRef.Card(cardId)
    Some(if claimant == states.now.game.current.walkActor then
      killedOwn(card, NoteArg.Card(vision))
    else killed(card, NoteArg.Player(claimant), NoteArg.Card(vision)))
