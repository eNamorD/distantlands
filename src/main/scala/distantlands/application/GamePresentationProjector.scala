package oathdigital.application

import oathdigital.catalog.{AdviserOnly, ExecutableCatalog, Locked, ModifierMark,
  PrintsPowers, SiteOnly}
import oathdigital.gameplay.PlayerSecretSummary
import oathdigital.gameplay.phases.rest.FinishRestProcedure
import oathdigital.gameplay.powers.{AdviserLimit, PlayerFacts, PowerImplementationStatus}
import oathdigital.gameplay.powers.wake.TrackerWake
import oathdigital.model._
import oathdigital.protocol.projection._

private[application] final class GamePresentationProjector(
    catalog: ExecutableCatalog
):
  // A card is implemented only once every power it declares is implemented
  // (see PowerImplementationStatus); a card with no declared power has
  // nothing to fall back on, so it reads as not yet implemented rather than
  // trivially done.
  private val powerImplemented = PowerImplementationStatus.implemented

  private def cardImplemented(definition: PrintsPowers): Boolean =
    definition.powers.nonEmpty && definition.powers.forall(power =>
      powerImplemented(power.id))

  def siteLabel(id: SiteId): String =
    catalog.site(id).fold(safeLabel(id.value))(_.name)
  def denizenLabel(id: DenizenId): String =
    catalog.denizen(id).fold(safeLabel(id.value))(_.name)
  def relicLabel(id: RelicId): String =
    catalog.relic(id).fold(safeLabel(id.value))(_.name)
  def edificeLabel(id: EdificeId, side: EdificeSide): String =
    catalog.edifice(id).fold(safeLabel(id.value)): edifice =>
      side match
        case EdificeSide.Intact => edifice.intact.name
        case EdificeSide.Ruined => edifice.ruined.name
  /** A player's display name: the one label the Players strip and the game
    * log both use, so the two never disagree. */
  /** A player is shown by their ID exactly as the host typed it. */
  def playerLabel(id: PlayerId): String = id.value

  private[application] def edificeCardDetails(value: EdificeState): CardDetailsProjection =
    val definition = catalog.edifice(value.id)
    val face = definition.map(e => value.side match {
      case EdificeSide.Intact => e.intact
      case EdificeSide.Ruined => e.ruined
    })
    CardDetailsProjection(value.id.value, "edifice", edificeLabel(value.id, value.side),
      suit = definition.map(_.suit.key),
      restrictions = face.map(restrictionName),
      rulesText = face.map(_.rulesText),
      side = Some(value.side match {
        case EdificeSide.Intact => "intact"
        case EdificeSide.Ruined => "ruined"
      }), favor = value.tokens.favor, secrets = value.tokens.secrets,
      implemented = face.exists(cardImplemented),
      powers = face.fold(Vector.empty)(powerFaces))

  def readyPlayers(ready: ReadyGame) = ready.game.current.players.map { player =>
    SetupPlayerProjection(player.player.value, playerLabel(player.player),
      roleKey(ready, player.player), ready.playerColors(player.player))
  }

  private def roleKey(ready: ReadyGame, player: PlayerId): String =
    Imperials.role(ready, player) match
      case Some(Role.Chancellor) => "chancellor"
      case Some(Role.Citizen) => "citizen"
      case _ => "exile"

  def readyWorld(ready: ReadyGame, viewer: Option[PlayerId]) =
    val current = ready.game.current
    Vector(
      region("cradle", Region.Cradle, current.map.cradle, ready, viewer),
      region("provinces", Region.Provinces, current.map.provinces, ready, viewer),
      region("hinterland", Region.Hinterland, current.map.hinterland, ready, viewer))

  private def region(id: String, where: Region, sites: Vector[SiteId],
      ready: ReadyGame, viewer: Option[PlayerId]): SetupRegionProjection =
    val current = ready.game.current
    // The pile is stored top last; the client reads it top first. A card is
    // a face only to a viewer whose knowledge records it: one who knew it
    // before it was discarded, or peeked it in the pile.
    val cards = current.commonCards.discard(where).reverse.map { card =>
      if identifiesCard(ready, viewer, card, None,
          CardContainer.RegionalDiscard(where))
      then cardDetails(card, None, hidden = false)
      else hiddenCard(cardKind(card))
    }
    SetupRegionProjection(id, sites.map(site =>
      siteProjection(site, current.map.sites.get(site), ready, viewer)),
      cards.size, cards.headOption.map(_.cardKind), cards)

  private def siteProjection(siteId: SiteId, state: Option[SiteState],
      ready: ReadyGame, viewer: Option[PlayerId]): SetupSiteProjection =
    val definition = catalog.site(siteId)
    SetupSiteProjection(siteId.value, siteLabel(siteId),
      state.fold(0)(_.tokens.favor), state.fold(0)(_.tokens.secrets),
      definition.fold(0)(_.capacity), definition.fold(0)(_.relicSlots),
      state.toVector.flatMap(_.denizens).map { denizen =>
        val label = denizen match {
          case value: DenizenState => denizenLabel(value.id)
          case value: EdificeState => edificeLabel(value.id, value.side)
        }
        val details = denizen match {
          case value: DenizenState => Some(cardDetails(value.id,
            Some(value.orientation), hidden = false).copy(
              favor = value.tokens.favor, secrets = value.tokens.secrets))
          case value: EdificeState => Some(edificeCardDetails(value))
        }
        SiteCardProjection(denizen.id.value, label, details)
      },
      SiteRelicsProjection(state.toVector.flatMap(_.relics).map(relic =>
        val known = viewer.exists(player =>
          ready.knowledge.siteRelics.getOrElse(player, Map.empty)
            .getOrElse(siteId, Vector.empty).contains(relic.id))
        if known then
          cardDetails(relic.id, Some(Orientation.FaceDown), hidden = false)
        else hiddenCard("relic"))),
      definition.fold(0)(_.defense),
      definition.flatMap(site => Option.when(site.forgeRequirements.isEmpty)(
        site.recoverDifficulty).flatten),
      definition.flatMap(_.forgeRequirements).map(tokens =>
        ForgeCostProjection(tokens.favor, tokens.secrets)),
      definition.toVector.flatMap(_.handlers).map(sitePower),
      state.flatMap(_.forces match {
        case value: SiteForces.Occupied => Some(forceProjection(value, ready))
        case SiteForces.Empty => None
      }))

  private def forceProjection(forces: SiteForces.Occupied,
      ready: ReadyGame): SiteForcesProjection =
    val ruler = SiteRule.ruler(forces, ready.game.current.players).fold(
      error => throw new IllegalStateException(s"invalid site ruler mapping: $error"),
      identity)
    forces.kind match
      case ForceKind.Exile(_) =>
        val SiteRuler.Player(playerId) = ruler: @unchecked
        val color = ready.playerColors.getOrElse(playerId,
          throw new IllegalStateException(
            s"missing color for site ruler ${playerId.value}"))
        SiteForcesProjection.Exile(forces.count, playerId.value, color,
          s"${color.key.capitalize} Warbands")
      case ForceKind.Imperial =>
        SiteForcesProjection.Imperial(forces.count, "Imperial Warbands")
      case ForceKind.Bandit =>
        SiteForcesProjection.Bandit(forces.count, "Bandit Warbands")

  def playerBoards(ready: ReadyGame, viewer: Option[PlayerId]) =
    val players = ready.game.current.players
    val start = viewer.flatMap(id => Option(players.indexWhere(_.player == id))
      .filter(_ >= 0)).getOrElse(0)
    (players.drop(start) ++ players.take(start)).map { player =>
      val secrets = PlayerSecretSummary.derive(ready, player.player)
        .fold(error => throw new IllegalStateException(error), identity)
      def identifies(id: CardId, orientation: Orientation,
          area: PlayerCardArea): Boolean = identifiesCard(ready, viewer, id,
        Some(orientation), CardContainer.Player(player.player, area))
      val kind = PlayerForceKind.of(ready, player)
      val printed = kind.fold(0)(ready.banks.warbandSupply.getOrElse(_, 0))
      val atSites = kind.fold(0)(PlayerFacts.atSites(ready, _))
      val banked = kind.fold(0)(PlayerFacts.banked(ready, _))
      val restBase = FinishRestProcedure.supplyGainAtRest(ready, player.player)
        .getOrElse(0)
      val bands = FinishRestProcedure.supplyRules(ready, player.player)
        .fold(Vector.empty)(_.refreshBands.map(band => SupplyBandProjection(
          band.warbandsInBank.minimum,
          Option.when(band.warbandsInBank.maximum != Int.MaxValue)(
            band.warbandsInBank.maximum),
          band.baseSupply)))
      PlayerBoardProjection(player.player.value, player.board.warbands,
        player.board.favor, player.board.faceUpSecrets, player.board.faceDownSecrets,
        secrets.committed, secrets.totalSecrets,
        player.board.supply.supply, player.pawnSite.map(_.value),
        player.advisers.map(card =>
          if identifies(card.id, adviserOrientation(card),
              PlayerCardArea.Advisers) then
            withTracking(withTokens(cardDetails(card.id,
              Some(adviserOrientation(card)), hidden = false), card match {
                case value: DenizenState => value.tokens
                case _ => Tokens.empty
              }), ready, player.player, card.id)
          else hiddenCard(cardKind(card.id))),
        player.relics.map(card =>
          if identifies(card.id, card.orientation, PlayerCardArea.Relics) then
            withWarbands(withTokens(cardDetails(card.id, Some(card.orientation),
              hidden = false), card.tokens), ready, card.id)
          else hiddenCard("relic")),
        player.revealedVisions.map(card => withWarbands(cardDetails(card.id,
          Some(card.orientation), hidden = false), ready, card.id)),
        banners(ready).filter(_.holderPlayerId.contains(player.player.value)),
        banked, printed, atSites, restBase, bands,
        AdviserLimit.drawn(catalog, ready, player.player))
    }

  /** The warbands standing on `card` (Obsidian Cage, False Prophet), one entry per
    * force: an Exile's by the colour of the player of that lineage. */
  private def withWarbands(details: CardDetailsProjection, ready: ReadyGame,
      card: CardId): CardDetailsProjection =
    val current = ready.game.current
    details.copy(warbands = current.warbandsOnCards.getOrElse(card, Map.empty)
      .toVector.filter(_._2 > 0).flatMap {
        case (ForceKind.Exile(lineage), count) => current.players
          .find(_.lineage == lineage).flatMap(player =>
            ready.playerColors.get(player.player).map(color =>
              SiteForcesProjection.Exile(count, player.player.value, color,
                s"${color.key.capitalize} Warbands")))
        case (ForceKind.Imperial, count) =>
          Some(SiteForcesProjection.Imperial(count, "Imperial Warbands"))
        case (ForceKind.Bandit, count) =>
          Some(SiteForcesProjection.Bandit(count, "Bandit Warbands"))
      }.sortBy(_.label))

  /** The pawn `holder` chose with the Tracker they hold, on that card
    * (catalog batch 10, slice 14). The choice was made openly, so every
    * viewer who sees the card sees it. */
  private def withTracking(details: CardDetailsProjection, ready: ReadyGame,
      holder: PlayerId, card: CardId): CardDetailsProjection =
    TrackerWake.tracked(ready, holder, card).flatMap(target =>
      ready.playerColors.get(target).map(color => details.copy(tracking =
        Some(TrackedPawnProjection(target.value, color,
          s"Tracking ${color.key.capitalize}")))))
      .getOrElse(details)

  private def withTokens(details: CardDetailsProjection,
      tokens: Tokens): CardDetailsProjection =
    details.copy(favor = tokens.favor, secrets = tokens.secrets)

  def banners(ready: ReadyGame): Vector[BannerProjection] =
    val current = ready.game.current
    Vector(
      BannerProjection("peoples-favor", current.banners.peoplesFavor.active match {
        case PeoplesFavorFace.Mob => "mob"
        case PeoplesFavorFace.GrandCouncil => "grand-council"
      }, current.banners.peoplesFavor.holder.map(_.value),
        current.banners.peoplesFavor.favor),
      BannerProjection("darkest-secret", current.banners.darkestSecret.active match {
        case DarkestSecretFace.WanderingFlame => "wandering-flame"
        case DarkestSecretFace.Festival => "festival"
      }, current.banners.darkestSecret.holder.map(_.value),
        current.banners.darkestSecret.secrets))

  /** `private[application]`, not `private`: `WalkerDecisionProjector`
    * (Task 5) is this redaction's third consumer, alongside [[playerBoards]]
    * below and the doc comment on [[identifiesCard]] that already describes
    * both -- a decision's subject card is redacted rather than dropped, the
    * same way a board slot is, so it reaches for the same substitute rather
    * than reproducing it.
    */
  private[application] def hiddenCard(kind: String) = CardDetailsProjection(
    "hidden", kind, s"Facedown $kind", orientation = Some("face-down"), hidden = true)

  /** Whether `viewer` may be told WHICH card this is, as opposed to merely
    * that a card is there.
    *
    * One rule with two consumers, which redact differently because they have
    * to. [[playerBoards]] above substitutes [[hiddenCard]] and keeps the slot
    * visible, since a board needs to show that an adviser is there. A walker
    * decision option cannot do that: its reference IS the card's identity and
    * the client answers by sending it back, so an unidentifiable option is not
    * an option at all and `WalkerDecisionProjector` suppresses the whole
    * decision instead. Both ask this one question rather than each writing its
    * own disclosure test.
    *
    * The clauses, and where each comes from:
    *
    *  - A site's denizens are named to every viewer whatever their
    *    orientation. That is not a judgement made here; `siteProjection`
    *    above already projects them that way, and a rule stricter than the
    *    world projection would hide a card the same response already names.
    *    Tighten both together or neither.
    *  - A site's facedown relics are named only to a viewer who has peeked
    *    (`knowledge.siteRelics`) or whose pawn stands at that site. The pawn
    *    clause is Recover's shipped disclosure: succeeding at Recover means
    *    looking through the site's facedown relics to choose one, which is
    *    why the relic decision could name them before this predicate existed.
    *    It is stated once, here, instead of being a blanket exemption.
    *  - A player's facedown cards are named to their owner, or to a viewer
    *    whose recorded `knowledge` covers them -- the same two conditions
    *    [[playerBoards]] applied inline before this method.
    *  - A player's temporary hand is named to that player and to nobody
    *    else. It is drawn-but-unresolved cards, not a board slot, and it is
    *    the one player area with no orientation to reason about.
    *  - A world card in the world deck or a discard pile is named only to a
    *    viewer whose knowledge records it: one who peeked at it there
    *    (Scryer, Oracular Pig), or, in a discard pile, one who knew it
    *    before it was discarded.
    *  - The gathering pile's cards are named to every viewer except the
    *    last three, the World task's facedown stack (CR p. 38). Autumn Wind
    *    gathers, shuffles and deals in one walk with no question between,
    *    so no viewer sees its pile.
    *  - A lineage's starting advisers are named only to the player seated
    *    with that lineage.
    *  - The Reliquary is named only to the Chancellor, and only in Setup,
    *    where they pick one of its relics (Empire ruling R29).
    *  - Everything else -- other decks, set-aside relics, the
    *    dispossessed pile, suited reserves, atlas sites -- is never
    *    identified. A card in a deck has no orientation at all, so it must be
    *    rejected by its container rather than by being facedown.
    */
  def identifiesCard(ready: ReadyGame, viewer: Option[PlayerId], id: CardId,
      orientation: Option[Orientation], container: CardContainer): Boolean =
    // Known-faceup, never merely "not facedown". A card whose container
    // holds no state at all -- a deck, a discard, a temporary hand -- has NO
    // orientation, and reading that absence as public is how such a card
    // would slip through a clause meant for a board slot.
    val faceup = orientation.contains(Orientation.FaceUp)
    container match
      case CardContainer.Site(_, SiteCardArea.Denizens) => true
      case CardContainer.Site(site, SiteCardArea.Relics) =>
        faceup || viewer.exists(player =>
          ready.knowledge.siteRelics.getOrElse(player, Map.empty)
            .getOrElse(site, Vector.empty).contains(id) ||
            ready.game.current.pawnSite(player).contains(site))
      // A temporary hand is the cards a player has drawn and not yet
      // resolved. It is private to them outright -- `PendingProjector`
      // projects a Search hand only to the drawing actor -- and its cards
      // carry no orientation to reason about, so ownership is the whole
      // rule and there is no faceup case to fall through to.
      case CardContainer.Player(owner, PlayerCardArea.Hand) =>
        viewer.contains(owner)
      case CardContainer.Player(owner, area) =>
        faceup || viewer.exists(player => player == owner ||
          knownToViewer(ready, player, id, area))
      // A world card known in the world deck or a discard pile. A card
      // entering the world deck is forgotten, and so is a shuffled pile's
      // (`CardKnowledgeMoves`, `PileOperations`), so a record here comes
      // from a peek (Scryer, Oracular Pig) or from knowing a card before it
      // was discarded.
      case CardContainer.Deck(CardDeck.World) | CardContainer.RegionalDiscard(_) =>
        viewer.exists(player => ready.knowledge.advisers
          .getOrElse(player, Vector.empty).exists(_ == id))
      // The World task deals two stacks faceup and one facedown (CR p. 38).
      // The facedown stack is the last three gathered cards, also after
      // the protected stack leaves; every card before it is on the table
      // for all to see.
      case CardContainer.CardGathering =>
        val pile = ready.game.current.cardGathering
        val at = pile.indexOf(id)
        at >= 0 && at < pile.size - 3
      // A starting adviser is kept facedown for the lineage's next player.
      case CardContainer.Lineage(lineage, LineageCardArea.StartingAdvisers) =>
        viewer.exists(player => ready.game.current.players
          .exists(seat => seat.player == player && seat.lineage == lineage))
      // The Chancellor chooses a Reliquary relic in Setup (CR p. 8, Empire
      // ruling R29); nobody else, and nobody later, sees the pile.
      case CardContainer.Reliquary =>
        ready.game.current.turn.phase == Phase.Setup &&
          viewer.exists(Imperials.chancellor(ready).contains)
      case _ => false

  /** [[identifiesCard]] for the card wherever it lies in `ready`: its own
    * orientation and container, looked up rather than passed. False for a
    * card that is nowhere. The game log asks this on the state before and
    * after each operation (spec, "Visibility").
    */
  def identifiesAt(ready: ReadyGame, viewer: Option[PlayerId], id: CardId)
      : Boolean =
    ready.game.cardIndex.toOption.flatMap(_.get(id)).exists(located =>
      identifiesCard(ready, viewer, id,
        GamePresentationProjector.orientationOf(located.state),
        located.location.container))

  /** A card's printed name; an edifice's is the side it shows in `ready`. */
  def cardLabel(ready: ReadyGame, id: CardId): String = id match
    case edifice: EdificeId =>
      val side = ready.game.cardIndex.toOption.flatMap(_.stateOf(edifice))
        .collect { case EdificeState(_, side, _) => side }
        .getOrElse(EdificeSide.Intact)
      edificeLabel(edifice, side)
    case other => cardDetails(other, None, hidden = false).name


  /** Recorded knowledge for the areas that have any: relics by
    * `heldRelics`, world cards by `advisers`. `Hand` never reaches here --
    * [[identifiesCard]] settles it on ownership alone.
    */
  private def knownToViewer(ready: ReadyGame, player: PlayerId, id: CardId,
      area: PlayerCardArea): Boolean = area match
    case PlayerCardArea.Relics =>
      ready.knowledge.heldRelics.getOrElse(player, Vector.empty).contains(id)
    case _ => ready.knowledge.advisers.getOrElse(player, Vector.empty)
      .exists(_.value == id.value)

  def adviserOrientation(card: AdviserState): Orientation = card match
    case value: DenizenState => value.orientation
    case value: VisionState => value.orientation
  def orientationName(value: Orientation): String = value match
    case Orientation.FaceUp => "face-up"
    case Orientation.FaceDown => "face-down"
  def cardKind(card: CardId): String = card match
    case _: VisionId => "vision"
    case _ => "denizen"

  def cardDetails(id: CardId, orientation: Option[Orientation],
      hidden: Boolean): CardDetailsProjection = id match
    case value: DenizenId => catalog.denizen(value).fold(
      CardDetailsProjection(value.value, "denizen", worldCardLabel(value),
        orientation = orientation.map(orientationName), hidden = hidden)) { d =>
      CardDetailsProjection(value.value, "denizen", d.name, Some(d.suit.key),
        Some(restrictionName(d)), Some(d.rulesText),
        orientation.map(orientationName), hidden = hidden,
        implemented = cardImplemented(d), powers = powerFaces(d))
    }
    case value: VisionId => VisionCardPresentation.byId.get(value.value).fold(
      CardDetailsProjection(value.value, "vision", safeLabel(value.value),
        orientation = orientation.map(orientationName), hidden = hidden)) { vision =>
      CardDetailsProjection(value.value, "vision", vision.name,
        rulesText = Some(vision.rulesText),
        orientation = orientation.map(orientationName), hidden = hidden)
    }
    case value: RelicId => catalog.relic(value).fold(
      CardDetailsProjection(value.value, "relic", safeLabel(value.value),
        orientation = orientation.map(orientationName), hidden = hidden)) { r =>
      CardDetailsProjection(value.value, "relic", r.name, rulesText = Some(r.rulesText),
        orientation = orientation.map(orientationName), relicValue = Some(r.value),
        defense = Some(r.defense), hidden = hidden, implemented = cardImplemented(r),
        powers = powerFaces(r))
    }
    case other => CardDetailsProjection(other.value, other.getClass.getSimpleName,
      safeLabel(other.value), orientation = orientation.map(orientationName), hidden = hidden)

  /** Each printed power as its card lays it out, for the inspection
    * overlay's braids. */
  private def powerFaces(card: PrintsPowers): Vector[PowerFaceProjection] =
    card.powers.map(power => PowerFaceProjection(power.text, power.costSymbols,
      power.persistent, power.modifier.map {
        case ModifierMark.Action(actions*) =>
          ModifierMarkProjection.Action(actions.map(_.key).toVector)
        case ModifierMark.BattlePlan(sides*) =>
          ModifierMarkProjection.BattlePlan(sides.map {
            case CampaignPlanSide.Attacker => "attack"
            case CampaignPlanSide.Defender => "defense" }.toVector)
      }))

  private def restrictionName(card: AnyRef): String = card match
    case _: (Locked & AdviserOnly) => "locked-adviser-only"
    case _: Locked => "locked"
    case _: SiteOnly => "site-only"
    case _: AdviserOnly => "adviser-only"
    case _ => "unrestricted"
  def safeLabel(id: String): String = id.split(":").lastOption.getOrElse(id)
    .split("-").map(_.capitalize).mkString(" ")
  private def worldCardLabel(id: WorldCardId): String = id match
    case value: DenizenId => denizenLabel(value)
    case value: VisionId => safeLabel(value.value)
  private def sitePower(handler: PowerId): SitePowerProjection =
    val kind = SitePowerText.kindOf(handler.value)
    SitePowerText.of(kind).fold(SitePowerProjection(kind, safeLabel(kind), None,
      handler.value))(printed => SitePowerProjection(kind, printed.label,
      Some(printed.text), handler.value))

private[application] object GamePresentationProjector:
  /** A card's orientation from its container state; `None` for a state
    * that has none (a deck, a discard, a hand). */
  def orientationOf(state: Option[CardState]): Option[Orientation] =
    state match
      case Some(DenizenState(_, orientation, _)) => Some(orientation)
      case Some(VisionState(_, orientation)) => Some(orientation)
      case Some(RelicState(_, orientation, _)) => Some(orientation)
      case _ => None
