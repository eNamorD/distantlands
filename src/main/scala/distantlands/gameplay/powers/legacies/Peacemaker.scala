package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.BannerRules
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.gameplay.powers.setup.BannerToSharedBank
import distantlands.model._

object PeacemakerCard extends Legacy(LegacyId("L04"), "Peacemaker",
    LegacyGoal.MostSites, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.peacemaker"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Return a banner you hold to the shared bank. " +
      "_Do not adjust its [favor] or [secret]._ If you do, remove " +
      "all warbands except the last _(including bandits)_ from " +
      "each site and player board, even yours.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Peacemaker (legacy L04), ACTION: return a banner the holder holds to
  * the shared bank, its favor or secrets left on it, then remove every
  * warband but the last from each site and each player board, the
  * holder's included (Legacy in-play powers design, ruling 8).
  *
  * Holding both banners, the holder chooses which; holding one, nothing is
  * asked. The use is refused while they hold none (implementation ruling
  * R4). The return is required: the removals follow only once the banner
  * has gone ("If you do"). Each removal is a `Kill`, as Ballot Box's
  * "remove" is: Imperial and Exile warbands return to their bank, bandits
  * to the bandit supply. Warbands on cards are neither at a site nor on a
  * board and stay.
  *
  * Line: "p2 returned the People's Favor with Peacemaker" (ruling 13),
  * read from the return's step.
  */
case object Peacemaker extends PaidAction(PeacemakerCard.power):
  val decisionId: String = "power.peacemaker.banner"
  private val notes = NoteSet(id)
  /** "{p2} returned the {People's Favor} with {Peacemaker}". */
  val returned: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" returned the "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The banners `player` holds, in banner order. */
  def banners(ready: ReadyGame, player: PlayerId): Vector[Banner] =
    Banner.all.filter(banner =>
      BannerRules.holder(ready.game.current, banner).contains(player))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(banners(ready, player).isEmpty)("You hold no banner")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) =>
      val held = banners(live, player)
      Option.when(held.size > 1)(Decide(decisionId, player,
        DecisionQuery.ChooseOne(held.map(banner =>
          DecisionOption.Banner(DecisionOptionRef.Banner(banner))),
          heading = Some("Peacemaker: return which banner to the shared " +
            "bank?")))).toVector),
    BuildOps((live, pending) => chosen(live, player, pending).map(banner =>
      BannerToSharedBank.move(live, banner).toVector), required = true),
    Note(id, line(_, player)),
    BuildOps((live, _) => Right(thinned(live))))))

  /** The banner to return: the only one held, or the one chosen. */
  private def chosen(ready: ReadyGame, player: PlayerId,
      pending: PendingTree): Either[OathViolation, Banner] =
    banners(ready, player) match
      case Vector(only) => Right(only)
      case held => PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Banner(banner) if held.contains(banner) =>
          banner }.toRight(PowerAnswers.missing(decisionId))

  /** Every warband but one at each site in play and on each board. */
  private def thinned(ready: ReadyGame): Vector[CoreOperation] =
    val current = ready.game.current
    val atSites: Vector[CoreOperation] = current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).map(_.forces).collect {
        case SiteForces.Occupied(kind, count) if count > 1 =>
          Kill(Piece.Warbands(kind, count - 1),
            PositionedLocation(Location.Site(site)))
      })
    val onBoards: Vector[CoreOperation] = current.players.flatMap(held =>
      PlayerForceKind.of(ready, held).filter(_ => held.board.warbands > 1)
        .map(kind => Kill(Piece.Warbands(kind, held.board.warbands - 1),
          PositionedLocation(Location.PlayArea(held.player)))))
    atSites ++ onBoards

  /** The banner the step before left the holder for the shared bank. */
  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    for
      (before, after) <- states.previous
      banner <- Banner.all.find(BannerToSharedBank.moved(before, after, _))
    yield returned.sourceless(NoteArg.Player(player), NoteArg.Banner(banner),
      NoteArg.Card(PeacemakerCard.id))
