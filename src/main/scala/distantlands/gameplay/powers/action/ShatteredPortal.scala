package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.dice.RollStep
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.model._

/** Shattered Portal (E14, ruined), ACTION: with the user's pawn at its site,
  * place 1 secret on it, roll the round die and place the pawn that many
  * sites Hinterward, the bottom Hinterland site wrapping to the top Cradle
  * site (catalog batch 10, slice 3).
  *
  * The roll is a `RollStep` of one round die; Jinx names only attack and
  * defense dice, so no reroll is offered. The placement is a plain `Move`,
  * as Stone Portal's is. The map holds 8 sites, so a roll of 1 to 6 never
  * lands on the starting site. With the pawn elsewhere (the user rules the
  * site), the option shows unavailable with [[NotHere]]. Its line: "Rolled
  * 4; {Blue} placed at {site}."
  */
case object ShatteredPortal extends PaidAction(StonePortalCard.ruined.power):
  val NotHere: String = "Your pawn is not at this site."
  val pool: PoolKey = PoolKey("shattered-portal")

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Rolled {4}; {Blue} placed at {site}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Rolled "), NotePart.Arg(0), NotePart.Text("; "),
    NotePart.Arg(1), NotePart.Text(" placed at "), NotePart.Arg(2),
    NotePart.Text(".")))

  private def portalSite(ready: ReadyGame): Option[SiteId] =
    ready.game.current.edificeSite(StonePortalCard.id, EdificeSide.Ruined)

  private def pawnHere(ready: ReadyGame, player: PlayerId): Boolean =
    portalSite(ready).exists(ready.game.current.pawnSite(player).contains)

  /** The site `steps` sites Hinterward of `from`, wrapping from the last
    * site in play to the first. */
  def destination(ready: ReadyGame, from: SiteId, steps: Int)
      : Option[SiteId] =
    val order = ready.game.current.map.hinterward
    Option(order.indexOf(from)).filter(_ >= 0)
      .map(index => order((index + steps) % order.size))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] = Option.when(!pawnHere(ready, player))(NotHere)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      ModifyDicePool(pool, 1),
      RollStep(pool, DiceSpec(DiceKind.Round), Some(player)),
      BuildOps((live, _) => moved(live, player)),
      Note(id, states => line(states, player)))))

  private def moved(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] = for
    from <- PawnMoves.pawnSite(ready, player)
    to <- destination(ready, from, RollResults.score(ready, pool))
      .toRight(OathViolation.InvalidEventOrder(
        s"${from.value} is not on the map"))
    ops <- PawnMoves.relocate(ready, player, to)
  yield ops

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    for site <- states.now.game.current.pawnSite(player)
    yield used(PowerSourceRef.Card(StonePortalCard.id),
      NoteArg.Number(RollResults.score(states.now, pool)),
      NoteArg.Player(player), NoteArg.Site(site))
