package oathdigital.testkit

import oathdigital.gameplay.actions.CardPlay
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.CardRefs.{CardRef, SiteRef}

/** The reads rule tests make of a state, named the way `Table` names what it
  * places. */
final class Look private (ready: ReadyGame):
  private def current = ready.game.current

  def player(p: PlayerId): PlayerState = current.player(p)
    .getOrElse(throw AssertionError(s"no player $p"))
  def supply(p: PlayerId): Int = player(p).board.supply.supply
  def favor(p: PlayerId): Int = player(p).board.favor

  /** The favor in `suit`'s bank. */
  def bankFavor(suit: Suit): Int = ready.banks.favor.getOrElse(suit, 0)

  /** Whether `card` lies in a regional discard pile. */
  def inRegionalDiscard(card: CardId): Boolean =
    current.commonCards.regionalDiscards.values.exists(_.contains(card))
  def faceUpSecrets(p: PlayerId): Int = player(p).board.faceUpSecrets
  def faceDownSecrets(p: PlayerId): Int = player(p).board.faceDownSecrets
  def boardWarbands(p: PlayerId): Int = player(p).board.warbands
  def advisers(p: PlayerId): Vector[CardId] = player(p).advisers.map(_.id)
  def relics(p: PlayerId): Vector[RelicId] = player(p).relics.map(_.id)
  def pawn(p: PlayerId): SiteId = player(p).pawnSite
    .getOrElse(throw AssertionError(s"$p has no pawn on the map"))

  /** The cards a site lists: its denizens and edifices. */
  def cardsAt(site: SiteRef): Vector[CardId] =
    current.map.sites(CardRefs.siteId(site)).denizens.map(_.id)
  def relicsAt(site: SiteRef): Vector[RelicId] =
    current.map.sites(CardRefs.siteId(site)).relics.map(_.id)
  def forces(site: SiteRef): SiteForces =
    current.map.sites(CardRefs.siteId(site)).forces
  def siteTokens(site: SiteRef): Tokens =
    current.map.sites(CardRefs.siteId(site)).tokens

  def phase: Phase = current.turn.phase
  def active: PlayerId = current.turn.activePlayer

  /** The tokens on `card`, at a site or in a player's play area. Fails when
    * the card lies in neither, where it could carry no tokens. */
  def tokensOn(card: CardRef)(using munit.Location): Tokens =
    val id = CardRefs.cardId(card)
    val onSites = current.map.sites.values.flatMap(s =>
      s.denizens.filter(_.id == id).map(_.tokens) ++
        s.relics.filter(_.id == id).map(_.tokens))
    val held = current.players.flatMap(p =>
      p.advisers.collect { case d: DenizenState if d.id == id => d.tokens } ++
        p.relics.filter(_.id == id).map(_.tokens))
    Unwrap.present((onSites ++ held).headOption,
      s"${CardRefs.nameOf(id)} at a site or in a play area")

  /** The tokens on `card` as `p`'s adviser. Fails when `p` holds no such
    * adviser. */
  def adviserTokens(p: PlayerId, card: CardRef)(
      using munit.Location): Tokens =
    val id = CardRefs.cardId(card)
    Unwrap.present(player(p).advisers.collectFirst {
      case d: DenizenState if d.id == id => d.tokens },
      s"${CardRefs.nameOf(id)} as ${p.value}'s adviser")

  /** The discard pile of the region after `site`'s region: where a card a
    * power discards from that site goes. Fails when the site is in no
    * region. */
  def discardAfter(site: SiteRef)(using munit.Location)
      : Vector[WorldCardId] =
    val id = CardRefs.siteId(site)
    val region = current.map.regionOf(id).getOrElse(
      munit.Assertions.fail(s"$id is in no region"))
    current.commonCards.discard(CardPlay.nextRegion(region))

  /** The kind of warband `p`'s own warbands are; fails with the rules'
    * refusal. */
  def forceKind(p: PlayerId)(using munit.Location): ForceKind =
    Unwrap.accepted(PlayerFacts.forceKind(ready, p))

  /** The cards in `region`'s discard pile, its top last. */
  def discardPile(region: Region): Vector[WorldCardId] =
    current.commonCards.discard(region)

  def peoplesFavorHolder: Option[PlayerId] = current.banners.peoplesFavor.holder
  /** The favor on the People's Favor. */
  def favorOnPeoplesFavor: Int = current.banners.peoplesFavor.favor
  def darkestSecretHolder: Option[PlayerId] = current.banners.darkestSecret.holder
  /** The secrets on the Darkest Secret. */
  def secretsOnDarkestSecret: Int = current.banners.darkestSecret.secrets

  /** Whether `viewer` knows `card` as an adviser. */
  def knowsAdviser(viewer: PlayerId, card: WorldCardId): Boolean =
    ready.knowledge.advisers.getOrElse(viewer, Vector.empty).contains(card)

  /** The dice in the roll pool `pool`; none while it is not open. */
  def diceInPool(pool: PoolKey): Int =
    current.rollPools.get(pool).fold(0)(_.count)

object Look:
  def apply(ready: ReadyGame): Look = new Look(ready)
  def apply(state: OathState)(using munit.Location): Look = state match
    case Ready(ready) => new Look(ready)
    case other => munit.Assertions.fail(s"expected a ready game, got $other")
