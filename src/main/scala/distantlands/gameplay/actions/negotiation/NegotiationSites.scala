package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.actions.VowOfWanderingRule
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer

/** One site an agreed deal moves: who gives it and who receives it. */
final case class SiteMove(giver: PlayerId, recipient: PlayerId, site: SiteId)

/** Site terms in a Negotiation deal (catalog batch 12, rulings 18 to 20 and
  * 27 to 29). The Negotiation reads it at its start and while the deal is
  * open, so it sits beside the action, outside `powers`; the power that
  * registers Deed Writer hooks no window.
  *
  * Deed Writer (faceup adviser) lets its holder give and receive sites
  * ([[DealRules.kinds]] opens the pairs that hold them). It is
  * non-persistent, so its use passes `restrictions`, the walk's power-use
  * restrictions (City Wall, Giant Python, Bandit Forest), judged as its
  * holder's own use. The Negotiation asks for no consent.
  */
object NegotiationSites:
  /** The players among `players` who hold Deed Writer faceup and may use
    * it: at most one. */
  def deedWriterUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    PowerAccess.faceupAdviserHolder(ready, DeedWriterCard.id).toVector
      .filter(holder => players.contains(holder) &&
        NegotiationReach.usable(catalog, ready, PowerUse.of(holder,
          DeedWriterCard.power, DeedWriterCard.id), restrictions))

  /** The sites `author` may give: the in-play sites their own-colour
    * warbands hold, in map order (rulings 18 and 28). Every Imperial
    * player's own colour is the Imperial warbands, so each may give every
    * Imperial site, on any turn. Bandits are nobody's colour. */
  def giftable(ready: ReadyGame, author: PlayerId): Vector[SiteId] =
    val current = ready.game.current
    current.player(author).flatMap(PlayerForceKind.of(ready, _))
      .fold(Vector.empty[SiteId])(own => current.map.inPlay.filter(id =>
        current.map.sites.get(id).exists(_.forces match
          case SiteForces.Occupied(kind, _) => kind == own
          case SiteForces.Empty => false)))

  /** Whether `author` may give a site to `recipient`, when the deal's
    * rules open sites between them: not when both are Imperial (ruling
    * 19), and not to a faceup Vow of Wandering holder, who cannot place
    * the warband ruling 20 requires. */
  def mayReceive(ready: ReadyGame, author: PlayerId,
      recipient: PlayerId): Boolean =
    !(Imperials.isImperial(ready, author) &&
      Imperials.isImperial(ready, recipient)) &&
      !VowOfWanderingRule.holds(ready, recipient)

  /** The sites the other participants' terms already give: an author may
    * not give one of them too, so the Chancellor and a Citizen never name
    * the same Imperial site. */
  def offeredByOthers(deal: DealState, author: PlayerId): Set[SiteId] =
    deal.participants.filter(_ != author).flatMap(other =>
      deal.terms(other).transfers.flatMap(_.sites)).toSet

  /** Every site the deal moves, by author in participant order, then in
    * the order each row names them: the order of the returns and of the
    * placement questions. */
  def moves(deal: DealState): Vector[SiteMove] =
    deal.participants.flatMap(author => deal.terms(author).transfers
      .flatMap(transfer => transfer.sites.map(SiteMove(author,
        transfer.recipient, _))))

  /** Each given site's warbands, all of them, moved to the old ruler's
    * board (ruling 20). */
  def returns(ready: ReadyGame, deal: DealState): Vector[CoreOperation] =
    moves(deal).flatMap(move => ready.game.current.map.sites.get(move.site)
      .map(_.forces).collect {
        case SiteForces.Occupied(kind, count) => Move(Piece.Warbands(kind,
          count), PositionedLocation(Location.Site(move.site)),
          PositionedLocation(Location.PlayArea(move.giver))): CoreOperation
      })

  /** The recipients the deal would leave with fewer warbands on their
    * board than sites they receive, counted after the warbands of the
    * sites they give come back (ruling 29), in participant order. */
  def shortOfWarbands(ready: ReadyGame, deal: DealState): Vector[PlayerId] =
    val all = moves(deal)
    deal.participants.filter { player =>
      val received = all.count(_.recipient == player)
      val returned = all.filter(_.giver == player)
        .map(move => standing(ready, move.site)).sum
      received > 0 && board(ready, player) + returned < received
    }

  /** The question that asks `site`'s new ruler how many warbands to place,
    * under the deal walk's `prefix`. */
  def warbandsDecisionId(prefix: String, site: SiteId): String =
    s"$prefix.warbands.${site.value}"

  /** How many warbands the new ruler placed at `site` in the deal under
    * `prefix`, once answered. */
  def placed(answered: Vector[Answered], prefix: String,
      site: SiteId): Option[Int] =
    val id = warbandsDecisionId(prefix, site)
    answered.collectFirst {
      case Answered(answeredId, ChooseAmountAnswer(count), _)
          if answeredId == id => count
    }

  /** The most `move`'s recipient may place at its site: the warbands their
    * board held once the returns ran, less what they placed at their
    * earlier sites, less one for each site still to come. Read from the
    * board plus every placement already answered, so a resumed walk asks
    * the same bound (a Branch is re-selected on resume). Never below 1:
    * the acceptance check keeps a warband for every site. */
  def most(ready: ReadyGame, deal: DealState, move: SiteMove,
      answered: Vector[Answered]): Int =
    val own = moves(deal).filter(_.recipient == move.recipient)
    val (earlier, rest) = own.span(_ != move)
    val afterReturns = board(ready, move.recipient) +
      own.flatMap(m => placed(answered, deal.prefix, m.site)).sum
    math.max(1, afterReturns - earlier.flatMap(m =>
      placed(answered, deal.prefix, m.site)).sum - (rest.size - 1))

  /** `count` of the recipient's own warbands, from their board to the
    * site (ruling 20). */
  def placement(ready: ReadyGame, move: SiteMove,
      count: Int): Option[CoreOperation] =
    ready.game.current.player(move.recipient)
      .flatMap(PlayerForceKind.of(ready, _)).map(kind => Move(
        Piece.Warbands(kind, count),
        PositionedLocation(Location.PlayArea(move.recipient)),
        PositionedLocation(Location.Site(move.site))))

  private def board(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.player(player).fold(0)(_.board.warbands)

  private def standing(ready: ReadyGame, site: SiteId): Int =
    ready.game.current.map.sites.get(site).fold(0)(_.forces.total)
