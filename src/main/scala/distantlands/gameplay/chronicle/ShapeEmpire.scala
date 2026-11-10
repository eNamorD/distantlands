package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.actions.{CardPlay, ProcedureNotes}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** Shape Empire, the Throne's second step (CR p. 37, Chronicle rulings 6, 9,
  * 11 and 14). The Throne holder may remove any of their warbands from
  * sites but the last one on the map; with no site, they clear their pawn's
  * site and place one there. Then every denizen at a site they do not rule
  * is discarded, except intact edifices and cards at Enduring sites. */
object ShapeEmpire:
  val removeDecisionId: String = "chronicle.shape-empire.remove"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val discarded: NoteKey = notes.key("discarded", Vector(NotePart.Arg(0),
    NotePart.Text(" was discarded from "), NotePart.Arg(1)))
  def noteKeys: Vector[NoteKey] = notes.declared

  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) => ChronicleTasks.forHolder(state,
      ChronicleTask.Throne)(removal(state, _))),
    Branch((state, _) => ChronicleTasks.forHolder(state,
      ChronicleTask.Throne)(discards(catalog, state, _))))

  private def removal(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    val current = state.game.current
    current.player(holder)
      .flatMap(PlayerForceKind.of(state, _)) match
      case None => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${holder.value} has no warbands")))
      case Some(kind) =>
        val ruled = current.map.inPlay.filter(ChronicleTasks.rules(state,
          holder, _)).flatMap(site => current.map.sites(site).forces match
            case SiteForces.Occupied(`kind`, count) => Some(site -> count)
            case _ => None)
        if ruled.isEmpty then fallback(state, holder, kind)
        else if ruled.map(_._2).sum <= 1 then Vector.empty
        else removeSome(holder, kind, ruled)

  /** With no site ruled: clear the pawn's site and place one warband. */
  private def fallback(state: ReadyGame, holder: PlayerId, kind: ForceKind)
      : Vector[Operation] =
    state.game.current.pawnSite(holder) match
      case None => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${holder.value} has no pawn on the map")))
      case Some(site) =>
        val at = PositionedLocation(Location.Site(site))
        val cleared = state.game.current.map.sites(site).forces match
          case SiteForces.Occupied(present, count) =>
            Vector(Kill(Piece.Warbands(present, count), at))
          case SiteForces.Empty => Vector.empty
        Vector(BuildOps((_, _) => Right(cleared :+ Move(
          Piece.Warbands(kind, 1),
          PositionedLocation(Location.WarbandBank(kind)), at)),
          required = true))

  private def removeSome(holder: PlayerId, kind: ForceKind,
      ruled: Vector[(SiteId, Int)]): Vector[Operation] = Vector(
    Decide(removeDecisionId, holder, DecisionQuery.Distribute(
      ruled.map((site, count) =>
        DistributeSlot(DecisionOptionRef.Site(site), 0, count, None)),
      0, ruled.map(_._2).sum - 1,
      heading = Some("Remove warbands from the Empire"),
      confirmLabel = "Remove")),
    BuildOps((_, pending) => PowerAnswers.distribution(pending,
      removeDecisionId).toRight(PowerAnswers.missing(removeDecisionId))
      .map(_.collect {
        case DistributeAmount(DecisionOptionRef.Site(site), amount)
            if amount > 0 => Move(Piece.Warbands(kind, amount),
          PositionedLocation(Location.Site(site)),
          PositionedLocation(Location.WarbandBank(kind)))
      }), required = true))

  /** Cards at Enduring sites are not discarded in Shape Empire (CR p. 37).
    * Ancient City is one; its handler names the keyword. */
  private def enduring(catalog: ExecutableCatalog, site: SiteId): Boolean =
    catalog.site(site).exists(_.handlers.exists(_.value.endsWith(".enduring")))

  /** Some legacies also protect denizens (ruling 6); they arrive with legacy
    * powers through this step's window. */
  private def discards(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[Operation] =
    val current = state.game.current
    val cards = current.map.inPlay.filterNot(site =>
      ChronicleTasks.rules(state, holder, site) || enduring(catalog, site))
      .flatMap(site => current.map.regionOf(site).toVector.flatMap(region =>
        current.map.sites(site).denizens.flatMap(card =>
          discard(catalog, site, region, card, holder)
            .map(op => (site, card.id, op)))))
    if cards.isEmpty then Vector.empty
    else BuildOps((_, _) => Right(cards.map(_._3)), required = true) +:
      cards.map((site, card, _) => Note(noteId, _ => Some(
        discarded.sourceless(NoteArg.Card(card), NoteArg.Site(site)))))

  private def discard(catalog: ExecutableCatalog, site: SiteId,
      region: Region, card: SiteDenizenState, holder: PlayerId)
      : Option[Discard.Denizen | Discard.RuinedEdifice] =
    val at = PositionedLocation(Location.Site(site))
    card match
      case DenizenState(id, _, tokens) => catalog.denizen(id).map(denizen =>
        Discard.Denizen(id, at, CardPlay.nextRegion(region), denizen.suit,
          tokens.favor, tokens.secrets, holder))
      case EdificeState(id, EdificeSide.Ruined, tokens) =>
        catalog.edifice(id).map(edifice => Discard.RuinedEdifice(id, at,
          edifice.suit, tokens.favor, tokens.secrets, holder))
      case EdificeState(_, EdificeSide.Intact, _) => None
