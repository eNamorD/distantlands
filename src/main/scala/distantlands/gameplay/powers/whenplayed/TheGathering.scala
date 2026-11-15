package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{Denizen, PrintedPower, SiteOnly}
import distantlands.gameplay.TurnOrder
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.action.PawnMoves
import distantlands.model._

object TheGatheringCard extends Denizen(DenizenId("27"), "The Gathering", Suit.Nomad) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.the-gathering"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** any players in turn order may put their " +
      "pawn on this site. Then, players with pawns here may " +
      "negotiate binding exchanges of [favor], [secret], relics, " +
      "and advisers.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Gathering (card 27, site-only), WHEN PLAYED: any players in turn
  * order may put their pawn on this site. Then, players with pawns here
  * may negotiate binding exchanges of favor, secrets, relics and advisers
  * (catalog batch 12, slice 6b).
  *
  * The order is the next round's, from the Chancellor, else the first
  * player (ruling 1). Each player whose pawn is elsewhere, the actor
  * included, is asked as the owner of their own question whether to put it
  * here; a yes moves it with a plain `Move`, not Travel (ruling 2), and it
  * stays. Every player keeps the same place in the tree whether or not
  * they are asked, so a later player's question resumes where it parked.
  * One line then names who is here. Then the deals ([[GatheringDeals]]).
  *
  * The site is the hook's: a site-only card's play names it in its
  * `RuleSourceRef.SiteCard`.
  */
case object TheGathering extends WhenPlayedPower:
  val cardId: DenizenId = TheGatheringCard.id
  val id: PowerId = TheGatheringCard.power.id
  val pawnDecisionId: String = "cardplay.the-gathering.pawn"
  val join: DecisionOptionRef.Button = DecisionOptionRef.Button("gathering.join")
  val stay: DecisionOptionRef.Button = DecisionOptionRef.Button("gathering.stay")
  private val notes = NoteSet(id)
  /** "Gathered at Ancient City: p1 and p2." Sourceless: the card-play line
    * before it names the card. */
  val gathered: NoteKey = notes.key("gathered", Vector(
    NotePart.Text("Gathered at "), NotePart.Arg(0), NotePart.Text(": "),
    NotePart.Arg(1), NotePart.Text(".")))
  /** "Nobody gathered at Ancient City." */
  val nobody: NoteKey = notes.key("nobody", Vector(
    NotePart.Text("Nobody gathered at "), NotePart.Arg(0),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The Gathered line names everyone here, so a pawn's answer needs no
    * "Chose" line: a yes is in the list, a no is not. */
  override def narratedDecisions: Set[String] = Set(pawnDecisionId)

  def effect(ctx: PowerCtx): Vector[Operation] = ctx.operation match
    case CardPlayedFaceup(_, _, RuleSourceRef.SiteCard(site, _), _) =>
      val order = TurnOrder.nextRoundOrder(ctx.state)
      val name = ctx.catalog.site(site).fold(site.value)(_.name)
      order.flatMap(pawn(site, name, _)) ++ Vector(
        Note(id, states => Some(line(states.now, site, order))),
        GatheringDeals.loop(ctx, site, order))
    case _ => Vector.empty

  /** The players whose pawns stand at `site`, in `order`. */
  def present(ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId]): Vector[PlayerId] =
    order.filter(ready.game.current.pawnSite(_).contains(site))

  /** `player`'s place: their question while their pawn is elsewhere, then
    * the move a yes makes. */
  private def pawn(site: SiteId, name: String,
      player: PlayerId): Vector[Operation] = Vector(
    Branch((live, _) => Option.when(
      !live.game.current.pawnSite(player).contains(site))(
      Decide(pawnDecisionId, player, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(join, s"Put my pawn on $name"),
        DecisionOption.Button(stay, "Stay")),
        heading = Some(s"The Gathering: put your pawn on $name?")))).toVector),
    BuildOps((live, pending) =>
      if joined(pending.answered, player) then
        PawnMoves.relocate(live, player, site)
      else Right(Vector.empty)))

  private def joined(answered: Vector[Answered], player: PlayerId): Boolean =
    answered.exists {
      case Answered(`pawnDecisionId`, DecisionAnswer.ChooseOneAnswer(`join`),
          `player`) => true
      case _ => false
    }

  private def line(ready: ReadyGame, site: SiteId,
      order: Vector[PlayerId]): LogNote =
    present(ready, site, order) match
      case Vector() => nobody.sourceless(NoteArg.Site(site))
      case here => gathered.sourceless(NoteArg.Site(site), NoteArg.Players(here))
