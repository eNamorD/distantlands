package oathdigital.gameplay.powers.travel

import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.operations.BoilingLakeCard
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._


/** Boiling Lake (E24, intact), an automatic rule of the intact face: "If
  * you travel to this site and do not rule this card, you must kill 2
  * warbands on your board if able." (catalog batch 9, slice 4)
  *
  * It follows Tyrant: after the traveller's pawn moves in the cost node, an
  * `AutomaticUse` kills two of the traveller's own warbands from their
  * board, when the traveller does not rule the Lake's site. "If able"
  * (rules 15.4): the kill is cut to what the board holds, and an empty
  * board kills nothing and writes nothing. It follows the pawn that moves,
  * so a Travel another player sent counts too (the FAQ: "Can I be forced
  * to travel here and lose warbands? Sure can."). Only Travel walks
  * `TravelCost`. The catalog marks it `persistent: false`, but it is a
  * must, so it keeps the default automatic resolution, as Tyrant does.
  *
  * Its line, in Tyrant's wording: "Killed 2 {Blue} warbands."
  */
case object BoilingLake extends ContributingPower:
  val edifice: EdificeId = BoilingLakeCard.id
  val id: PowerId = BoilingLakeCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  val Warbands: Int = 2
  private val notes = NoteSet(id)
  /** "Killed {2} {Blue} warbands." */
  val killed: NoteKey = notes.key("killed", Vector(NotePart.Text("Killed "),
    NotePart.Arg(0), NotePart.Text(" "), NotePart.Arg(1),
    NotePart.Plural(0, " warband.", " warbands.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      TravelRoute.pawnMove(ctx.operation).fold(operations)(route =>
        operations :+ AutomaticUse(route.player, id, edifice, (_, _) =>
          Vector(kill(route.player), Note(id, killedNote(_, route.player))))))))

  /** The Lake's site, when `route` arrives there and its traveller does not
    * rule it. */
  override def applicable(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).exists(route =>
      EdificeSetupSupport.siteOf(ctx.state, edifice, EdificeSide.Intact)
        .exists(site => route.destination == site &&
          !SiteRulers.rulerOf(ctx.state, site)
            .contains(SiteRuler.Player(route.player))))

  private def boardWarbands(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.players.find(_.player == player)
      .fold(0)(_.board.warbands)

  /** Up to two of the traveller's own warbands, from their board. */
  private def kill(traveller: PlayerId): Operation = BuildOps((ready, _) =>
    PlayerFacts.forceKind(ready, traveller).map(kind =>
      val lost = math.min(Warbands, boardWarbands(ready, traveller))
      if lost == 0 then Vector.empty
      else Vector[CoreOperation](Kill(Piece.Warbands(kind, lost),
        PositionedLocation(Location.PlayArea(traveller))))))

  /** The warbands the step before killed on the traveller's board. */
  private def killedNote(states: NoteStates, traveller: PlayerId)
      : Option[LogNote] = for
    (before, after) <- states.previous
    lost = boardWarbands(before, traveller) - boardWarbands(after, traveller)
    if lost > 0
  yield killed(PowerSourceRef.Card(edifice), NoteArg.Number(lost),
    NoteArg.Player(traveller))
