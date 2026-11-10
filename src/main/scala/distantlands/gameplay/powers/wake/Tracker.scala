package distantlands.gameplay.powers.wake

import distantlands.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.{PhasePower, QueuedStarts}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.{PaidAction, PawnMoves}
import distantlands.model._

object TrackerCard extends Denizen(DenizenId("221"), "Tracker", Suit.Discord) with AdviserOnly:
  val action = PrintedPower(PowerId("denizen.tracker.action"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Choose an enemy pawn at your site and end your " +
      "Act Phase.")
  val wake = PrintedPower(PowerId("denizen.tracker.wake"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** If you chose a pawn last turn, you may place your " +
      "pawn at their site.")
  val powers: Vector[PrintedPower] = Vector(action, wake)

/** Tracker (card 221), ACTION: "Choose an enemy pawn at your site and end
  * your Act Phase." (catalog batch 10, slice 14).
  *
  * The choice is recorded with `TrackPawn`, under the user, and the Act
  * phase ends with `EnterPhase(Rest)`, as Wizard School ends it. The record
  * outlives the turn; the user's next End Wake clears it. An enemy is
  * another player who is not Imperial alongside the user. With no enemy
  * pawn at the site, the menu shows "No valid enemy pawn". The line is
  * "Tracked {Red}."; the phase change shows in the lines around it.
  */
case object TrackerAction extends PaidAction(TrackerCard.action):
  val decisionId: String = "power.tracker.target"
  private val notes = NoteSet(id)
  val tracked: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Tracked "), NotePart.Arg(0), NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The enemies of `player` whose pawns stand at `player`'s site. */
  def targets(ready: ReadyGame, player: PlayerId): Vector[PlayerId] =
    ready.game.current.pawnSite(player).toVector.flatMap(site =>
      ready.game.current.players.collect {
        case other if other.pawnSite.contains(site) &&
            Imperials.enemies(ready, player, other.player) => other.player })

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(targets(ready, player).isEmpty)(PawnMoves.noValidEnemyPawn)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      Branch((live, _) => Vector(Decide(decisionId, player,
        DecisionQuery.ChooseOne(targets(live, player).map(target =>
          DecisionOption.Player(DecisionOptionRef.Player(target))),
          heading = Some("Tracker: choose an enemy pawn at your site"))))),
      BuildOps((_, pending) => PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Player(target) =>
          Vector[CoreOperation](TrackPawn(player, target))
      }.toRight(PowerAnswers.missing(decisionId))),
      Note(id, states => for
        card <- PowerSourceRef.of(source)
        case DecisionOptionRef.Player(target) <-
          NoteSupport.answer(states, decisionId)
      yield tracked(card, NoteArg.Player(target))),
      EnterPhase(Phase.Rest))))

/** Tracker (card 221), WAKE: "If you chose a pawn last turn, you may place
  * your pawn at their site." (catalog batch 10, slice 14, ruling 6).
  *
  * Optional: a usable power in the Wake menu, never run on its own. It
  * places the pawn at the tracked pawn's site at that moment with a plain
  * `Move`, as Passages does, so no Travel rule reads it. Phase-power access
  * already requires Tracker to be the user's faceup adviser. The line is
  * Passages': "{Blue} placed at {site}."
  */
case object TrackerWake extends PhasePower:
  val id: PowerId = TrackerCard.wake.id
  def timing: PowerTiming = PowerTiming.Wake
  /** The reason with no record from last turn. */
  val noPawnChosen: String = "No pawn was chosen last turn"
  /** The reason with the user's pawn already at the tracked pawn's site. */
  val alreadyThere: String = "Already at the chosen pawn's site"
  private val notes = NoteSet(id)
  val placed: NoteKey = PawnMoves.placedKey(notes, NoteKey.Used)
  override def noteKeys: Vector[NoteKey] = notes.declared

  /** The pawn `holder` chose, when `card` is the Tracker that records it. */
  def tracked(ready: ReadyGame, holder: PlayerId, card: CardId)
      : Option[PlayerId] =
    if card == TrackerCard.id then ready.game.current.trackedPawns.get(holder)
    else None

  def usable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean = true

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    val current = ready.game.current
    current.trackedPawns.get(player) match
      case None => Some(noPawnChosen)
      case Some(target) => Option.when(
        current.pawnSite(target) == current.pawnSite(player))(alreadyThere)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      BuildOps((live, _) => for
        target <- live.game.current.trackedPawns.get(player).toRight(
          OathViolation.InvalidEventOrder(
            s"${player.value} chose no pawn last turn"))
        site <- PawnMoves.pawnSite(live, target)
        moves <- PawnMoves.relocate(live, player, site)
      yield moves),
      Note(id, PawnMoves.placedNote(placed, source, player)))))
