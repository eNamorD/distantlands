package oathdigital.gameplay

import oathdigital.gameplay.actions.BannerRules
import oathdigital.model._

/** The rulebook's access rule for using a card's power: cards at your site,
  * cards at sites you rule, and, for the Bandit Crown's holder, cards at
  * sites bandits rule (`BanditCrownRule`); for the Grand Mask's wearer,
  * cards at Imperial sites, which the Imperials then reach only from their
  * pawn's site (`GrandMaskRule`); and everything in your play area
  * (relics, banners, advisers and legacies). One definition, used by phase
  * powers and by every walker contribution's `applicable`.
  *
  * Denizens at sites are always faceup and relics at sites are always
  * facedown, so a site relic never grants access. Both faces of an edifice
  * count. Faceup advisers and relics only, unless the caller opts in to
  * facedown advisers (battle plans).
  *
  * A modifier may make its user act as if their pawn stood at another site
  * for the rest of the action ([[asIfDecision]]). `ActAsIf` records the
  * answer as walker scratch (`CurrentGameState.walkerAsIfSite`), and the
  * walk's end clears it. For the walk's actor, that site then takes the
  * pawn site's place for `locate` ([[presentSite]]). Ruled sites, the
  * Crown holder's bandit sites and the play area stay usable; the physical
  * pawn site does not, unless it is one of those.
  *
  * Master of Disguise makes its user act as if they held another player's
  * advisers instead of their own for the rest of a Trade. `ActAsIfAdvisers`
  * records the player as walker scratch
  * (`CurrentGameState.walkerAsIfAdvisers`), and the walk's end clears it.
  * For the walk's actor, that player's advisers then replace their own in
  * `locate`, `accessible` and `holdsFaceupAdviser` ([[advisersOf]]).
  */
private[gameplay] object PowerAccess:
  sealed trait Held extends Product with Serializable
  object Held:
    final case class AtSite(site: SiteId) extends Held
    case object InPlayArea extends Held

  /** The player whose advisers `player` acts with: the one Master of
    * Disguise chose, for the walk's actor while `ActAsIfAdvisers` holds
    * (`CurrentGameState.walkerAsIfAdvisers`), and `player` otherwise. */
  def advisingPlayer(ready: ReadyGame, player: PlayerId): PlayerId =
    val current = ready.game.current
    current.walkerAsIfAdvisers.filter(_ => current.walkActor == player)
      .getOrElse(player)

  /** The advisers `player` acts with ([[advisingPlayer]]). A reader that
    * decides whether an adviser's power serves or binds a player reads
    * these. A physical fact, such as a move, the adviser limit or the
    * board shown, reads `PlayerState.advisers`. */
  def advisersOf(ready: ReadyGame, player: PlayerId): Vector[AdviserState] =
    ready.game.current.player(advisingPlayer(ready, player)).toVector
      .flatMap(_.advisers)

  /** The player who really holds `card` faceup among their advisers, if
    * any. It reads no [[advisersOf]]: no Trade reads it, and a card a
    * Master of Disguise holder acts with would otherwise have two
    * holders. */
  def faceupAdviserHolder(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    ready.game.current.players.find(player => faceUp(player.advisers, card))
      .map(_.player)

  /** Whether `player` acts with `card` faceup among their advisers
    * ([[advisersOf]]). */
  def holdsFaceupAdviser(ready: ReadyGame, player: PlayerId, card: DenizenId)
      : Boolean =
    faceUp(advisersOf(ready, player), card)

  private def faceUp(advisers: Vector[AdviserState], card: DenizenId)
      : Boolean =
    advisers.exists {
      case DenizenState(held, Orientation.FaceUp, _) => held == card
      case _ => false
    }

  /** The player holding `relic` faceup in their play area, if any. */
  def faceupRelicHolder(ready: ReadyGame, relic: RelicId): Option[PlayerId] =
    ready.game.current.players.find(_.relics.exists {
      case RelicState(`relic`, Orientation.FaceUp, _) => true
      case _ => false
    }).map(_.player)

  def ruledSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ready.game.current.map.inPlay.filter(site =>
      SiteRule.rulingPlayer(ready, site)
        .contains(SiteRuler.Player(actor))).toSet

  /** The decision a modifier asks to name the site its user acts as if at
    * for the rest of the action. Only one such modifier is picked per
    * action. `ActAsIf` records its answer as state. */
  val asIfDecision: String = "modifier.as-if-site"

  /** The site `actor` acts as if at for the rest of the walk, if any. Only
    * the walk's actor acts from it; every other player acts from their
    * pawn (catalog batch 8, item 13c). */
  def asIfSite(ready: ReadyGame, actor: PlayerId): Option[SiteId] =
    val current = ready.game.current
    current.walkerAsIfSite.filter(_ => current.walkActor == actor)

  /** Where the actor acts from: the as-if site, else the pawn site. */
  def presentSite(ready: ReadyGame, actor: PlayerId): Option[SiteId] =
    asIfSite(ready, actor).orElse(ready.game.current.pawnSite(actor))

  /** The sites whose cards `actor` uses as their ruler would: the sites
    * they rule, less those the Grand Mask takes from the Imperials during
    * its wearer's turn; the bandit sites for the Bandit Crown's holder; and
    * the Imperial sites for the Mask's wearer. A count of ruled sites reads
    * `ruledSites`, which neither relic changes. The wearer's sites include
    * their battle plans, which stay the Empire's: plans are reached
    * through `PlanContext`, not through access. */
  def rulingSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    ruledSites(ready, actor) -- GrandMaskRule.maskedSites(ready) ++
      BanditCrownRule.sitesFor(ready, actor) ++
      GrandMaskRule.sitesFor(ready, actor)

  private def usableSites(ready: ReadyGame, actor: PlayerId): Set[SiteId] =
    rulingSites(ready, actor) ++ presentSite(ready, actor)

  def accessible(ref: RuleSourceRef, face: RuleSourceFace, ready: ReadyGame,
      actor: PlayerId, facedownAdviser: Boolean = false): Boolean =
    lazy val sites = usableSites(ready, actor)
    ref match
      case RuleSourceRef.Site(id) =>
        ready.game.current.pawnSite(actor).contains(id)
      case RuleSourceRef.SiteCard(id, _) =>
        face == RuleSourceFace.FaceUp && sites(id)
      case RuleSourceRef.SiteRelic(_, _) => false
      case RuleSourceRef.Edifice(id, _) =>
        (face == RuleSourceFace.Intact || face == RuleSourceFace.Ruined) &&
          sites(id)
      case RuleSourceRef.Adviser(owner, _) =>
        owner == advisingPlayer(ready, actor) &&
        (face == RuleSourceFace.FaceUp ||
          (facedownAdviser && face == RuleSourceFace.FaceDown))
      case RuleSourceRef.Relic(owner, _) =>
        owner == actor && face == RuleSourceFace.FaceUp
      case RuleSourceRef.Banner(key) => Banner.fromKey(key).exists(banner =>
        BannerRules.holder(ready.game.current, banner).contains(actor))
      case RuleSourceRef.Foundation(_) => true
      case RuleSourceRef.Legacy(lineage, _) =>
        ready.game.current.player(actor)
          .exists(_.lineage == lineage) && face == RuleSourceFace.Active
      case _ => false

  /** Where `card` is, if `actor` may use it. A site card or edifice at a
    * usable site is `AtSite`: a site whose cards the actor uses as a ruler
    * ([[rulingSites]]), or the one they act from ([[presentSite]]). A faceup
    * adviser the actor acts with ([[advisersOf]]) or a faceup relic in their
    * play area is `InPlayArea`.
    */
  def locate(ready: ReadyGame, actor: PlayerId, card: CardId)
      : Option[Held] =
    val current = ready.game.current
    val atSite = usableSites(ready, actor).toVector
      .sortBy(_.value).collectFirst:
      case id if current.map.sites.get(id).exists(_.denizens.exists {
        case d: DenizenState => d.id == card && d.orientation == Orientation.FaceUp
        case e: EdificeState => e.id == card
      }) => Held.AtSite(id)
    lazy val inPlay = advisersOf(ready, actor).exists {
        case d: DenizenState => d.id == card && d.orientation == Orientation.FaceUp
        case _ => false
      } || current.player(actor).exists(_.relics.exists(relic =>
        relic.id == card && relic.orientation == Orientation.FaceUp))
    atSite.orElse(Option.when(inPlay)(Held.InPlayArea))

  def siteOf(ready: ReadyGame, actor: PlayerId, card: CardId): Option[SiteId] =
    locate(ready, actor, card).flatMap:
      case Held.AtSite(site) => Some(site)
      case Held.InPlayArea => ready.game.current.pawnSite(actor)
