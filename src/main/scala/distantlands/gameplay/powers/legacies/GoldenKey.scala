package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

object GoldenKeyCard extends Legacy(LegacyId("L12"), "Golden Key",
    LegacyGoal.GrandScepter, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.golden-key"),
    persistent = false, cost = Cost(favorBurnt = 1, secretBurnt = 1),
    text = "**IF YOU ARE AN IMPERIAL:** You may always peek at the " +
      "Reliquary. **ACTION:** Burn [secret-burnt] [favor-burnt] to " +
      "take a relic from the Reliquary.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Golden Key (legacy L12): "If you are an Imperial: you may always peek
  * at the Reliquary. ACTION: Burn a secret and a favor to take a relic
  * from the Reliquary" (Legacy in-play powers design, rulings 2 and 10).
  *
  * Both halves need its holder to be an Imperial when they are used
  * (implementation ruling R1): the peek is a clause of
  * `GamePresentationProjector.identifiesCard`, which reads [[peeker]], and
  * an Exile holder's menu leaves the ACTION out. The ACTION takes any
  * relic in the Reliquary the holder chooses, facedown to their play
  * area, as the Chancellor takes one at Setup. It is not a Recover. The
  * use is refused while the Reliquary in play is empty, a set-aside one
  * included (R4, R5).
  *
  * Line: "p3 took a relic from the Reliquary with Golden Key" (ruling
  * 13), which never names the relic.
  */
case object GoldenKey extends PaidAction(GoldenKeyCard.power):
  val decisionId: String = "power.golden-key.relic"
  private val notes = NoteSet(id)
  /** "{p3} took a relic from the Reliquary with {Golden Key}". */
  val took: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" took a relic from the Reliquary with "),
    NotePart.Arg(1)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The seated Imperial holding Golden Key active, read now (ruling 2). */
  def peeker(ready: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(ready, GoldenKeyCard.id,
      ChronicleLegacyRules.imperialRoles)

  override def listed(ready: ReadyGame, player: PlayerId): Boolean =
    peeker(ready).contains(player)

  override def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    peeker(ready).contains(player)

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(ready.game.campaign.reliquaryInPlay.isEmpty)(
      "The Reliquary is empty")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(live.game.campaign.reliquaryInPlay.map(relic =>
        DecisionOption.Relic(DecisionOptionRef.Relic(relic))),
        heading = Some("Golden Key: take a relic from the Reliquary"),
        confirmVerb = Some("Take"))))),
    BuildOps((live, pending) => take(live, player, pending), required = true),
    Note(id, _ => Some(took.sourceless(NoteArg.Player(player),
      NoteArg.Card(GoldenKeyCard.id)))))))

  private def take(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId).collect {
      case DecisionOptionRef.Relic(relic)
          if ready.game.campaign.reliquaryInPlay.contains(relic) =>
        Vector[CoreOperation](Move(Piece.Card(relic),
          PositionedLocation(Location.Reliquary),
          PositionedLocation(Location.PlayArea(player)),
          resultingOrientation = Some(Orientation.FaceDown)))
    }.toRight(PowerAnswers.missing(decisionId))
