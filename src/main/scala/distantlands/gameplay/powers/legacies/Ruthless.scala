package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

object RuthlessCard extends Legacy(LegacyId("L29"), "Ruthless",
    LegacyGoal.HighestRelic, Suit.Order):
  val power = PrintedPower(PowerId("legacy.ruthless"),
    persistent = false, cost = Cost(favorBurnt = 2),
    text = "**ACTION:** Burn [favor-burnt] [favor-burnt] to bury a " +
      "denizen at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ruthless (legacy L29), ACTION: burn 2 favor, then bury a denizen at the
  * holder's site (Legacy in-play powers design, ruling 7).
  *
  * Any card in the site's denizen row may be buried: a denizen, or an
  * edifice on either face, locked or not, since a bury ignores locks (CR:
  * "You can bury locked cards, including edifices"). Its tokens return as
  * `Bury.standard` returns them. The question is always asked; the use is
  * refused while the site holds no such card (implementation ruling R4).
  * The bury is required, so a card a restriction keeps (Ancient
  * Bloodline) is not offered.
  *
  * Line: "p1 buried Herbalist with Ruthless" (ruling 13), covering the
  * generic Buried line.
  */
case object Ruthless extends PaidAction(RuthlessCard.power):
  val decisionId: String = "power.ruthless.denizen"
  private val notes = NoteSet(id)
  /** "{p1} buried {card} with {Ruthless}". */
  val buried: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" buried "), NotePart.Arg(1), NotePart.Text(" with "),
    NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The cards in the denizen row of `player`'s site, with the site. */
  def targets(ready: ReadyGame, player: PlayerId)
      : Vector[(SiteId, SiteDenizenState)] =
    val current = ready.game.current
    current.pawnSite(player).toVector.flatMap(site =>
      current.map.sites.get(site).toVector.flatMap(_.denizens)
        .map(site -> _))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(targets(ready, player).isEmpty)("No denizen at your site")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(targets(live, player).map((_, card) =>
        option(card)), heading = Some("Ruthless: bury which card at your " +
          "site?"))))),
    BuildOps((live, pending) => bury(catalog, live, player, pending),
      required = true),
    Note(id, line(_, player), covers = true))))

  private def refOf(card: SiteDenizenState): DecisionOptionRef = card match
    case held: DenizenState => DecisionOptionRef.Denizen(held.id)
    case held: EdificeState => DecisionOptionRef.Edifice(held.id)

  private def option(card: SiteDenizenState): DecisionOption = card match
    case held: DenizenState =>
      DecisionOption.Denizen(DecisionOptionRef.Denizen(held.id))
    case held: EdificeState =>
      DecisionOption.Edifice(DecisionOptionRef.Edifice(held.id))

  private def bury(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    for
      ref <- PowerAnswers.one(pending, decisionId)
        .toRight(PowerAnswers.missing(decisionId))
      found <- targets(ready, player).find((_, card) => refOf(card) == ref)
        .toRight(OathViolation.InvalidEventOrder(
          s"${ref.wireId} is not a card at the holder's site"))
      (site, card) = found
      (buryable, tokens) = card match
        case held: DenizenState => (BuryableCard.Denizen(held.id), held.tokens)
        case held: EdificeState => (BuryableCard.Edifice(held.id), held.tokens)
      suit = catalog.suitOf(buryable.id)
      _ <- Either.cond(tokens.favor == 0 || suit.isDefined, (),
        OathViolation.InvalidEventOrder(
          s"no suit is known for ${buryable.id.value}"))
    yield Bury.standard(buryable, PositionedLocation(Location.Site(site)),
      suit, tokens.favor, tokens.secrets, player)

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    NoteSupport.answer(states, decisionId).flatMap(DecisionOptionRef.cardOf)
      .map(card => buried.sourceless(NoteArg.Player(player),
        NoteArg.Card(card), NoteArg.Card(RuthlessCard.id)))
