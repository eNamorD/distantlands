package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.{PlayerFacts, PowerAnswers}
import oathdigital.model._

object FalseProphetCard extends Denizen(DenizenId("85"), "False Prophet", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.false-prophet"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** if you're an Exile, gain one warband and " +
      "put it on any revealed Vision. You now also have it " +
      "revealed. If it is ever discarded, kill the warband and " +
      "play or discard the Vision as if you had searched.")
  val powers: Vector[PrintedPower] = Vector(power)

/** False Prophet's When Played (card 85; catalog batch 11, ruling 14 and
  * the False Prophet readings): an Exile with a warband in their bank
  * chooses another player's revealed Vision, gains one warband and puts it
  * on that Vision. The warband claims it: `RevealedVisions.of` counts it
  * as theirs too. With no such Vision, no warband or no Exile role it does
  * nothing and writes nothing. What a discard of the Vision does is
  * [[FalseProphetClaim]]'s.
  *
  * The Branch keeps its shape once the question is answered, and the
  * question's options do not read the bank, so a walk that resumes after
  * the placement, when the bank may be empty, finds the same nodes.
  */
case object FalseProphet extends WhenPlayedPower:
  val cardId: DenizenId = FalseProphetCard.id
  val id: PowerId = FalseProphetCard.power.id
  val decisionId: String = "power.false-prophet.vision"

  private val notes = NoteSet(id)
  /** "Put a warband on {Blue}'s {Vision of Faith}." */
  val put: NoteKey = notes.key("put", Vector(
    NotePart.Text("Put a warband on "), NotePart.Arg(0),
    NotePart.Text("'s "), NotePart.Arg(1), NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    val actor = ctx.actor
    Vector(Branch((live, pending) =>
      if chosen(pending.answered).isEmpty && !placeable(live, actor) then
        Vector.empty
      else Vector[Operation](question(live, actor), place(actor),
        Note(id, putNote, covers = true))))

  /** Every other player's revealed Vision, in seat order. */
  private def targets(ready: ReadyGame, actor: PlayerId): Vector[VisionId] =
    ready.game.current.players.filterNot(_.player == actor)
      .flatMap(_.revealedVisions.map(_.id))

  /** Whether `actor` is an Exile with a warband in their bank and some
    * other player has revealed a Vision. */
  private def placeable(ready: ReadyGame, actor: PlayerId): Boolean =
    Imperials.role(ready, actor).contains(Role.Exile) &&
      PlayerFacts.forceKind(ready, actor).toOption
        .exists(kind => PlayerFacts.banked(ready, kind) > 0) &&
      targets(ready, actor).nonEmpty

  private def question(ready: ReadyGame, actor: PlayerId): Decide =
    Decide(decisionId, actor, DecisionQuery.ChooseOne(
      targets(ready, actor).map(vision =>
        DecisionOption.Vision(DecisionOptionRef.Vision(vision))),
      heading = Some("False Prophet: put a warband on a revealed Vision")))

  private def chosen(answered: Vector[Answered]): Option[VisionId] =
    PowerAnswers.last(answered, decisionId).collect {
      case DecisionOptionRef.Vision(vision) => vision }

  /** The gain and the move onto the chosen Vision. */
  private def place(actor: PlayerId): Operation = BuildOps((live, pending) =>
    chosen(pending.answered).fold[Either[OathViolation,
        Vector[CoreOperation]]](Right(Vector.empty))(vision =>
      PlayerFacts.forceKind(live, actor).map(kind => Vector(
        Gain.Warbands(actor, kind, 1),
        Move(Piece.Warbands(kind, 1),
          PositionedLocation(Location.PlayArea(actor)),
          PositionedLocation(Location.OnCard(vision)))))))

  private def putNote(states: NoteStates): Option[LogNote] = for
    vision <- chosen(states.answered)
    owner <- states.now.game.current.players
      .find(_.revealedVisions.exists(_.id == vision)).map(_.player)
  yield put(PowerSourceRef.Card(cardId), NoteArg.Player(owner),
    NoteArg.Card(vision))
