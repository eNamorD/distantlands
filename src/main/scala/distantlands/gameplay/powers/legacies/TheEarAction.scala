package distantlands.gameplay.powers.legacies

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

/** The Ear (legacy L19), its ACTION half: "You may peek at the relics at
  * any site" (Legacy in-play powers design, ruling 12). Its card and its
  * Chronicle half are `TheEarCard` and `TheEar`.
  *
  * Free. The holder picks a site in play with a relic missing from their
  * recorded knowledge (`knowledge.siteRelics`), their own site included
  * until they peek it (implementation ruling R2), and peeks at all its
  * relics. They know them while they stay at that site, as with the minor
  * peek; a relic that moves carries its knowledge as `CardKnowledgeMoves`
  * carries it. The use is refused while no site qualifies (R4).
  *
  * Line: "p2 peeked at the relics at Deep Woods with The Ear" (R3),
  * public, never naming the relics, and covering the peeks' lines.
  */
case object TheEarAction extends PaidAction(TheEarCard.power):
  val decisionId: String = "power.the-ear.site"
  private val notes = NoteSet(id)
  /** "{p2} peeked at the relics at {site} with {The Ear}". */
  val peeked: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" peeked at the relics at "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The sites in play holding a relic `player` has not peeked, in map
    * order. */
  def sites(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
    val map = ready.game.current.map
    val known = ready.knowledge.siteRelics.getOrElse(player, Map.empty)
    map.inPlay.filter(site => map.sites.get(site).exists(_.relics.exists(
      relic => !known.getOrElse(site, Vector.empty).contains(relic.id))))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(sites(ready, player).isEmpty)(
      "No site holds a relic you have not seen")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(sites(live, player).map(site =>
        DecisionOption.Site(DecisionOptionRef.Site(site))),
        heading = Some("The Ear: peek at the relics at which site?"))))),
    BuildOps((live, pending) => peeks(live, player, pending),
      required = true),
    Note(id, line(_, player), covers = true))))

  private def peeks(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId).collect {
      case DecisionOptionRef.Site(site) if sites(ready, player).contains(site) =>
        site
    }.toRight(PowerAnswers.missing(decisionId)).map(site =>
      ready.game.current.map.sites.get(site).toVector.flatMap(_.relics)
        .map(relic => Peek(player, relic.id, Location.Site(site))))

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    PowerAnswers.last(states.answered, decisionId).collect {
      case DecisionOptionRef.Site(site) => peeked.sourceless(
        NoteArg.Player(player), NoteArg.Site(site),
        NoteArg.Card(TheEarCard.id))
    }
