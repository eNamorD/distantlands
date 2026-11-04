package oathdigital.gameplay.powers.action

import oathdigital.catalog.{ExecutableCatalog, PrintedPower, Relic}
import oathdigital.gameplay.{NoteSupport, SiteRulers}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object ImperialSealCard extends Relic(RelicId("R26"), "Imperial Seal", value = 35, defense = 2):
  val power = PrintedPower(PowerId("relic.imperial-seal"),
    persistent = false, cost = Cost(secret = 1),
    text = "**ACTION:** If no Imperial pawns are at your site, move any " +
      "number of Imperial warbands from your site to any Imperial " +
      "site or Imperial player board.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Imperial Seal (R26): "ACTION: If no Imperial pawns are at your site,
  * move any number of Imperial warbands from your site to any Imperial site
  * or Imperial player board." (catalog batch 10, slice 1).
  *
  * It is usable when no Imperial player's pawn stands at the user's site
  * and Imperial warbands are there, the user's own pawn counting when they
  * are Imperial. After the secret it asks how many, from 1, then one
  * destination (ruling 4): another Imperial site
  * ([[SiteRulers.imperialSites]]) or an Imperial player's board. Its line
  * covers the generic move line: "Moved 2 Imperial warbands to {site}." or
  * "... to {Purple}'s board."
  */
case object ImperialSeal extends PaidAction(ImperialSealCard.power):
  val countDecisionId: String = "power.imperial-seal.count"
  val destinationDecisionId: String = "power.imperial-seal.destination"
  private val notes = NoteSet(id)
  /** "Moved {0} Imperial warband(s) to {1}." */
  val toSite: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Moved "), NotePart.Arg(0), NotePart.Text(" Imperial "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" to "),
    NotePart.Arg(1), NotePart.Text(".")))
  /** "Moved {0} Imperial warband(s) to {1}'s board." */
  val toBoard: NoteKey = notes.key("used.board", Vector(
    NotePart.Text("Moved "), NotePart.Arg(0), NotePart.Text(" Imperial "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" to "),
    NotePart.Arg(1), NotePart.Text("'s board.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(countDecisionId, destinationDecisionId)

  /** The Imperial warbands at `site`. */
  def imperialAt(ready: ReadyGame, site: SiteId): Int =
    ready.game.current.map.sites.get(site).map(_.forces) match
      case Some(SiteForces.Occupied(ForceKind.Imperial, n)) => n
      case _ => 0

  private def imperialPawnAt(ready: ReadyGame, site: SiteId): Boolean =
    ready.game.current.players.exists(state =>
      ready.game.current.pawnSite(state.player).contains(site) &&
        Imperials.isImperial(ready, state.player))

  /** Where the warbands at `site` may go. */
  private def destinations(ready: ReadyGame, site: SiteId)
      : Vector[DecisionOption] =
    SiteRulers.imperialSites(ready).filterNot(_ == site).map(other =>
      DecisionOption.Site(DecisionOptionRef.Site(other))) ++
      Imperials.players(ready).map(player =>
        DecisionOption.Player(DecisionOptionRef.Player(player)))

  override def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    ready.game.current.pawnSite(player).exists(site =>
      !imperialPawnAt(ready, site) && imperialAt(ready, site) > 0 &&
        destinations(ready, site).nonEmpty)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    ready.game.current.pawnSite(player).toRight(
      OathViolation.StartUnavailable("Your pawn is not on the map")).map(
      site => Sequence(Vector[Operation](
        Branch((live, _) => Vector(Decide(countDecisionId, player,
          DecisionQuery.ChooseAmount(1, imperialAt(live, site),
            Some("Imperial Seal: move how many Imperial warbands?"),
            "Move")))),
        Branch((live, _) => Vector(Decide(destinationDecisionId, player,
          DecisionQuery.ChooseOne(destinations(live, site),
            heading = Some("Imperial Seal: move them where?"))))),
        BuildOps((_, pending) => moved(site, pending), required = true),
        Note(id, movedNote(_, site, source), covers = true))))

  private def moved(site: SiteId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] = for
    count <- PowerAnswers.amount(pending, countDecisionId)
      .toRight(PowerAnswers.missing(countDecisionId))
    to <- PowerAnswers.one(pending, destinationDecisionId).collect {
      case DecisionOptionRef.Site(other) => Location.Site(other)
      case DecisionOptionRef.Player(board) => Location.PlayArea(board)
    }.toRight(PowerAnswers.missing(destinationDecisionId))
  yield Vector(Move(Piece.Warbands(ForceKind.Imperial, count),
    PositionedLocation(Location.Site(site)), PositionedLocation(to)))

  /** The move just made, read as the warbands that left `site`. */
  private def movedNote(states: NoteStates, site: SiteId,
      source: DecisionOptionRef): Option[LogNote] = for
    card <- PowerSourceRef.of(source)
    (before, _) <- states.previous
    count = imperialAt(before, site) - imperialAt(states.now, site)
    if count > 0
    to <- NoteSupport.answer(states, destinationDecisionId)
    line <- to match
      case DecisionOptionRef.Site(other) =>
        Some(toSite(card, NoteArg.Number(count), NoteArg.Site(other)))
      case DecisionOptionRef.Player(board) =>
        Some(toBoard(card, NoteArg.Number(count), NoteArg.Player(board)))
      case _ => None
  yield line
