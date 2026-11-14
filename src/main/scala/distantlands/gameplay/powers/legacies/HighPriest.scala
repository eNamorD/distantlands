package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.BannerRules
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

object HighPriestCard extends Legacy(LegacyId("L21"), "High Priest",
    LegacyGoal.DarkestSecret, Suit.Order):
  val power = PrintedPower(PowerId("legacy.high-priest"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** If you have the Darkest Secret, you may move " +
      "any number of [secret] from the Darkest Secret to your " +
      "player board. **WHEN NEGOTIATING:** You may give [secret].")
  val powers: Vector[PrintedPower] = Vector(power)

/** High Priest (legacy L21), its ACTION: "If you have the Darkest Secret,
  * you may move any number of secrets from the Darkest Secret to your
  * player board" (Legacy in-play powers design, ruling 11).
  *
  * The use is refused unless the holder holds the Darkest Secret with a
  * secret on it (implementation ruling R4). The holder chooses an amount
  * from 1 to the secrets on it, always asked, and that many move to their
  * board faceup with a `Take`, so a restriction on taking applies. Its
  * WHEN NEGOTIATING half is slice 2's (ruling 19).
  *
  * Line: "p2 moved 3 secrets from the Darkest Secret with High Priest"
  * (R3).
  */
case object HighPriest extends PaidAction(HighPriestCard.power):
  val decisionId: String = "power.high-priest.secrets"
  private val notes = NoteSet(id)
  /** "{p2} moved {3 secrets} from the Darkest Secret with {High Priest}". */
  val moved: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" moved "), NotePart.Arg(1),
    NotePart.Text(" from the Darkest Secret with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  private def holds(ready: ReadyGame, player: PlayerId): Boolean =
    BannerRules.holder(ready.game.current, Banner.DarkestSecret)
      .contains(player)

  private def secrets(ready: ReadyGame): Int =
    ready.game.current.banners.darkestSecret.secrets

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    if !holds(ready, player) then Some("You do not hold the Darkest Secret")
    else Option.when(secrets(ready) == 0)(
      "The Darkest Secret holds no secrets")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseAmount(1, secrets(live),
        Some("High Priest: move how many secrets to your board?"),
        "Move secrets")))),
    BuildOps((live, pending) => move(live, player, pending), required = true),
    Note(id, line(_, player)))))

  private def move(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.amount(pending, decisionId)
      .filter(amount => amount >= 1 && amount <= secrets(ready))
      .map(amount => Vector[CoreOperation](Take(Piece.Secrets(amount),
        player, Location.OnBanner(Banner.DarkestSecret),
        Location.PlayArea(player))))
      .toRight(PowerAnswers.missing(decisionId))

  /** The secrets the step before took from the banner. */
  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    for
      (before, after) <- states.previous
      count = secrets(before) - secrets(after)
      if count > 0
    yield moved.sourceless(NoteArg.Player(player),
      NoteArg.Amount(count, NoteUnit.Secret), NoteArg.Card(HighPriestCard.id))
