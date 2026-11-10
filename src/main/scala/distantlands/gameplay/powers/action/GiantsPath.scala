package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.{IgnoresOwnLock, QueuedStarts}
import distantlands.gameplay.powers.{PowerAnswers, SiteRoom}
import distantlands.model._

/** Giant's Path (E09, intact), ACTION: place 1 secret on this card, move it
  * to another site, then place the user's pawn there (catalog batch 10,
  * slice 4).
  *
  * The Path shows the lock icon. Its use ignores its own lock
  * ([[IgnoresOwnLock]]), and nothing else does. The destination needs an
  * empty slot (batch 4, "Site capacity"), so the question lists
  * `SiteRoom.open` less the Path's own site. With no such site, the option
  * shows unavailable with [[NoRoom]]. The card moves by a plain `Move`,
  * keeping its tokens, and the pawn is placed, not moved by Travel, as
  * Giant's Rampage places it. Its line: "Giant's Path and {Blue} now stand
  * at {site}."
  */
case object GiantsPath extends PaidAction(GiantsPathCard.intact.power)
    with IgnoresOwnLock:
  val ownCard: CardId = GiantsPathCard.id
  val decisionId: String = "power.giants-path.site"
  val heading: String = "Giant's Path: move to which site?"
  val NoRoom: String = "No other site has room for Giant's Path."

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Giant's Path and {Blue} now stand at {site}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Giant's Path and "), NotePart.Arg(0),
    NotePart.Text(" now stand at "), NotePart.Arg(1), NotePart.Text(".")))

  private def pathSite(ready: ReadyGame): Option[SiteId] =
    ready.game.current.edificeSite(GiantsPathCard.id, EdificeSide.Intact)

  /** The sites in play with room for the Path, less its own, in map
    * order. */
  def destinations(catalog: ExecutableCatalog, ready: ReadyGame)
      : Vector[SiteId] =
    val here = pathSite(ready)
    SiteRoom.open(catalog, ready).filterNot(here.contains)

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(destinations(catalog, ready).isEmpty)(NoRoom)

  override def narratedDecisions: Set[String] = Set(decisionId)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      Branch((live, _) => Vector(Decide(decisionId, player,
        DecisionQuery.ChooseOne(destinations(catalog, live).map(site =>
          DecisionOption.Site(DecisionOptionRef.Site(site))),
          heading = Some(heading))))),
      BuildOps((live, pending) => moved(live, player, pending),
        required = true),
      Note(id, line(_, player), covers = true))))

  /** The Path's move to the answered site, then the pawn's placement. */
  private def moved(ready: ReadyGame, player: PlayerId,
      pending: PendingTree): Either[OathViolation, Vector[CoreOperation]] =
    for
      to <- PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Site(site) => site }.toRight(
        OathViolation.InvalidEventOrder("Giant's Path's site is unanswered"))
      from <- pathSite(ready).toRight(OathViolation.InvalidEventOrder(
        "Giant's Path is not on the map"))
      placed <- PawnMoves.relocate(ready, player, to)
    yield Move(Piece.Card(GiantsPathCard.id),
      PositionedLocation(Location.Site(from)),
      PositionedLocation(Location.Site(to)), by = Some(player)) +: placed

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    states.now.game.current.pawnSite(player).map(site =>
      used(PowerSourceRef.Card(GiantsPathCard.id), NoteArg.Player(player),
        NoteArg.Site(site)))
