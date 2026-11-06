package oathdigital.gameplay.powers.action

import oathdigital.catalog.{AdviserOnly, Denizen, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.{EffectiveSuit, NoteSupport}
import oathdigital.gameplay.actions.CardPlay
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.powers.rest.HolderAdviserLimit
import oathdigital.model._

object AssassinCard extends Denizen(DenizenId("80"), "Assassin", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.assassin"),
    persistent = false, cost = Cost(favor = 1),
    text = "You can only have two advisers. **ACTION:** Discard a " +
      "faceup adviser of a player whose pawn is at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Assassin (card 80, adviser-only, locked): "You can only have two
  * advisers. ACTION: Discard a faceup adviser of a player whose pawn is at
  * your site."
  *
  * The limit is a [[HolderAdviserLimit]], as Silver Tongue's and Insomnia's
  * are, so it is registered in the walker catalog too, and `AdviserLimit`
  * reads it. It writes no line.
  *
  * The ACTION places 1 favor on this card, then offers the faceup denizen
  * advisers of every player whose pawn is at the player's site, the
  * player's own included, in seat then adviser order, read live. The chosen
  * card gets the standard discard: to the discard pile of the region after
  * the player's, its favor to its suit's bank and its secrets to the player
  * facedown. The discard is a required batch, so the Locked restriction
  * hides a locked adviser, Assassin among them, and the question shows
  * Continue when nothing can be chosen. With nothing to discard the cost
  * stays paid and one line says so. Its line covers the generic Discarded line.
  */
case object Assassin extends PaidAction(AssassinCard.power)
    with ContributingPower:
  val decisionId: String = "power.assassin.adviser"
  /** How many advisers the holder may have, in either orientation. */
  val HolderLimit: Int = 2
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Discarded {card} from {Blue}'s advisers." */
  val discarded: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0), NotePart.Text(" from "),
    NotePart.Arg(1), NotePart.Text("'s advisers.")))
  /** "No faceup adviser could be discarded." */
  val nothing: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("No faceup adviser could be discarded.")))

  private val limit = HolderAdviserLimit(AssassinCard.id, HolderLimit)

  def source: RuleSourceRef = RuleSourceRef.GameRule(this.id.value)
  override def narratedDecisions: Set[String] = Set(decisionId)

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.SearchPlayAdviser -> Vector(limit.contribution))

  /** The adviser limit Assassin sets on `player`: its holder, and only
    * while it is faceup. */
  def limitFor(ready: ReadyGame, player: PlayerId): Option[Int] =
    limit.limitFor(ready, player)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => ask(live, player)),
    BuildOps((live, pending) => discard(catalog, live, player, pending),
      required = true),
    Note(this.id, discardedNote(_, source), covers = true))))

  private final case class Candidate(owner: PlayerId, card: DenizenState)

  /** The faceup denizen advisers of the players at the player's site, the
    * player's own included, in seat then adviser order. */
  private def candidates(ready: ReadyGame, actor: PlayerId): Vector[Candidate] =
    ready.game.current.pawnSite(actor).toVector.flatMap(site => for
      held <- ready.game.current.players if held.pawnSite.contains(site)
      card <- held.advisers.collect {
        case faceup @ DenizenState(_, Orientation.FaceUp, _) => faceup }
    yield Candidate(held.player, card))

  private def ask(ready: ReadyGame, actor: PlayerId): Vector[Operation] =
    Vector(Decide(decisionId, actor, DecisionQuery.ChooseOne(
      candidates(ready, actor).map(candidate =>
        DecisionOption.Denizen(DecisionOptionRef.Denizen(candidate.card.id))),
      heading = Some("Assassin: discard a faceup adviser of a player at " +
        "your site")), passWhenEmpty = true))

  /** No answer means nothing could be discarded. */
  private def discard(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId) match
      case None => Right(Vector.empty)
      case Some(ref) =>
        for
          chosen <- candidates(ready, actor)
            .find(candidate => DecisionOptionRef.Denizen(candidate.card.id) == ref)
            .toRight(OathViolation.InvalidEventOrder(
              s"${ref.wireId} is not an adviser Assassin can discard"))
          suit <- EffectiveSuit.of(catalog, ready, chosen.card.id)
            .toRight(OathViolation.UnknownWorldCard(chosen.card.id))
          region <- ready.game.current.pawnSite(actor)
            .flatMap(ready.game.current.map.regionOf).map(CardPlay.nextRegion)
            .toRight(OathViolation.PawnSiteMissing(actor))
        yield Vector[CoreOperation](Discard.Denizen(chosen.card.id,
          PositionedLocation(Location.PlayArea(chosen.owner)), region, suit,
          chosen.card.tokens.favor, chosen.card.tokens.secrets, actor,
          required = true))

  /** The card discarded and its owner, read before the discard. */
  private def discardedNote(states: NoteStates, source: DecisionOptionRef)
      : Option[LogNote] = PowerSourceRef.of(source).map(card => (for
    case DecisionOptionRef.Denizen(chosen) <-
      NoteSupport.answer(states, decisionId)
    (before, _) <- states.previous
    owner <- before.game.current.players
      .find(_.advisers.exists(_.id == chosen))
  yield discarded(card, NoteArg.Card(chosen), NoteArg.Player(owner.player)))
    .getOrElse(nothing(card)))
