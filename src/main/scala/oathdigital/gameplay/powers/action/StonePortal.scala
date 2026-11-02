package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.gameplay.PowerAccess
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._

object StonePortalCard extends Edifice(EdificeId("E14"), Suit.Arcane):
  object intact extends EdificeFace("Stone Portal") with Locked:
    val power = PrintedPower(PowerId("edifice.e14.intact"),
      persistent = false, cost = Cost(secret = 1),
      text = "**ACTION:** If your pawn is at this site, place your pawn " +
        "on another site. If your pawn is not at this site, place " +
        "your pawn at this site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Shattered Portal"):
    val power = PrintedPower(PowerId("edifice.e14.ruined"),
      persistent = false, cost = Cost(secret = 1),
      text = "**ACTION:** If your pawn is at this site, roll [round-die] " +
        "and place your pawn that many sites Hinterward. The bottom " +
        "Hinterland site wraps to the top Cradle site.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Stone Portal (E14, intact), ACTION: place 1 secret on this card, then,
  * with the user's pawn at the Portal's site, place it on another site of
  * their choice; from elsewhere (the user rules the Portal's site), place
  * it at the Portal's site without asking (catalog batch 9, slice 6).
  *
  * The engine's access rule gates it: the Portal is usable at the pawn's
  * site or at a site the user rules. The placement is a plain `Move`, not a
  * Travel, as Whistle and Magic Carpet place pawns. Its line is theirs:
  * "{Blue} placed at {site}." The ruined face, Shattered Portal, needs the
  * round die and is not implemented.
  */
case object StonePortal extends PaidAction(StonePortalCard.intact.power):
  val edifice: EdificeId = StonePortalCard.id
  val decisionId: String = "power.stone-portal.site"
  private val notes = NoteSet(id)
  val placed: NoteKey = PawnMoves.placedKey(notes, NoteKey.Used)
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** Where the intact Portal stands. */
  def portalSite(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, edifice, EdificeSide.Intact)

  private def pawnHere(ready: ReadyGame, player: PlayerId): Boolean =
    portalSite(ready).exists(PowerAccess.pawnSite(ready, player).contains)

  def build(ready: ReadyGame, player: PlayerId, source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => portalSite(live).toVector.filter(_ =>
      pawnHere(live, player)).map(site => PawnMoves.siteChoice(decisionId,
        player, PawnMoves.sitesOtherThan(live, site),
        "Stone Portal: place your pawn at which site?"))),
    BuildOps((live, pending) => destination(live, player, pending)
      .flatMap(PawnMoves.relocate(live, player, _))),
    Note(id, PawnMoves.placedNote(placed, source, player)))))

  /** The chosen site when the pawn stood here, else the Portal's site. */
  private def destination(ready: ReadyGame, player: PlayerId,
      pending: PendingTree): Either[OathViolation, SiteId] =
    if pawnHere(ready, player) then PawnMoves.chosenSite(pending, decisionId)
    else portalSite(ready).toRight(OathViolation.InvalidEventOrder(
      "Stone Portal is not on the map"))
