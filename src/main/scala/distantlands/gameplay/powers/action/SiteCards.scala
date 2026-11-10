package oathdigital.gameplay.powers.action

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.actions.CardPlay
import oathdigital.gameplay.actions.cardplay.CardSlots
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** The cards of `suits` at a site that a power may discard, for Dark
  * Enforcer, Taming Charm and Great Feast: faceup denizens and ruined
  * edifices, in site order, each with its suit. An intact edifice is locked
  * and stays, and a facedown card has no suit. Mounted Library discards
  * itself through `discard`.
  *
  * The standard discard sends a denizen to the discard pile of the region
  * after the site's, and sets a ruined edifice aside (Chronicle ruling 9).
  * Either returns its favor to its suit's bank and its secrets to the
  * acting player facedown.
  */
private[powers] final class SiteCards(suits: Set[Suit]):

  def at(catalog: ExecutableCatalog,
      ready: ReadyGame, site: SiteId): Vector[(SiteDenizenState, Suit)] =
    ready.game.current.map.sites.get(site).toVector.flatMap(_.denizens)
      .flatMap(card => discardable(card).flatMap(catalog.suitOf)
        .filter(suits).map(card -> _))

  private def discardable(card: SiteDenizenState): Option[CardId] = card match
    case DenizenState(id, Orientation.FaceUp, _) => Some(id)
    case EdificeState(id, EdificeSide.Ruined, _) => Some(id)
    case _ => None

  /** The standard discard of `card`, of `suit`, from `site`. `cause` is
    * the power's use that discards it; a judgment passes none. */
  def discard(ready: ReadyGame, site: SiteId, card: SiteDenizenState,
      suit: Suit, actor: PlayerId, required: Boolean,
      cause: Option[DiscardCause])
      : Either[OathViolation, CoreOperation] =
    val from = PositionedLocation(Location.Site(site))
    card match
      case denizen: DenizenState => ready.game.current.map.regionOf(site)
        .map(CardPlay.nextRegion)
        .toRight(OathViolation.InvalidEventOrder(
          s"${site.value} is not in play"))
        .map(region => Discard.Denizen(denizen.id, from, region, suit,
          denizen.tokens.favor, denizen.tokens.secrets, actor, required,
          cause = cause))
      case edifice: EdificeState => Right(Discard.RuinedEdifice(edifice.id,
        from, suit, edifice.tokens.favor, edifice.tokens.secrets, actor,
        required, cause = cause))

  def option(card: SiteDenizenState): DecisionOption = card match
    case denizen: DenizenState =>
      DecisionOption.Denizen(DecisionOptionRef.Denizen(denizen.id))
    case edifice: EdificeState =>
      DecisionOption.Edifice(DecisionOptionRef.Edifice(edifice.id))

  def refOf(card: SiteDenizenState): DecisionOptionRef = option(card).ref

  /** `card`'s option as the printed restrictions judge `actor`'s discard of
    * it from `site` (consent design, slice 3). It is kept as it is when
    * nothing refuses, askable when only waivable refusals stop it, shown
    * unavailable with a protection's reason, or left out. */
  def judged(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId, site: SiteId,
      card: SiteDenizenState, suit: Suit): Option[DecisionOption] =
    CardSlots.judge(catalog, ready, actor, option(card),
      discard(ready, site, card, suit, actor, required = true, cause = None)
        .map(Vector(_)))

  /** The question over the cards at the player's site, each judged for the
    * player's discard. With none that may be chosen it is the Continue
    * screen, where an askable card still asks (catalog batch 8, slice 10,
    * part 2b). */
  def ask(catalog: ExecutableCatalog,
      ready: ReadyGame, player: PlayerId, decisionId: String,
      heading: String): Vector[Operation] =
    val found = ready.game.current.pawnSite(player).toVector.flatMap(site =>
      at(catalog, ready, site).flatMap((card, suit) =>
        judged(catalog, ready, player, site, card, suit)))
    Vector(Decide(decisionId, player, DecisionQuery.ChooseOne(found,
      heading = Some(heading)), passWhenEmpty = true))

  /** The answered card, with its site and suit, while it is still at the
    * player's site. Nothing when the question was not asked, or passed. */
  def chosen(catalog: ExecutableCatalog,
      ready: ReadyGame, player: PlayerId, pending: PendingTree,
      decisionId: String): Option[(SiteId, SiteDenizenState, Suit)] = for
    answer <- PowerAnswers.one(pending, decisionId)
    site <- ready.game.current.pawnSite(player)
    (card, suit) <- at(catalog, ready, site)
      .find((card, _) => refOf(card) == answer)
  yield (site, card, suit)
