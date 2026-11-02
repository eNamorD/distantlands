package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.gameplay.PowerAccess
import oathdigital.model._

object SpiralCastleCard extends Edifice(EdificeId("E23"), Suit.Hearth):
  object intact extends EdificeFace("Spiral Castle") with Locked:
    val power = PrintedPower(PowerId("edifice.e23.intact"),
      persistent = false, cost = Cost.free,
      text = "If you're victorious in a raid, place the defender's pawn " +
        "on this site, and they lose 2 Supply.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Spiral Dungeons"):
    val power = PrintedPower(PowerId("edifice.e23.ruined"),
      persistent = false, cost = Cost.free,
      text = "If you're victorious, place the attacker's pawn on this " +
        "site, and they lose 2 Supply. _Bandits will use this._")
    val powers: Vector[PrintedPower] = Vector(power)

/** What both faces of the Spiral edifice (E23) do once their user has won
  * (catalog batch 9, slice 7): the loser's pawn is placed at the face's
  * site, or stays when already there, and they lose 2 Supply, cut to what
  * they have. The line covers the pawn's move it restates: "Sent {Red}'s
  * pawn to {site}; they lost 2 Supply." */
private[campaign] object SpiralSteps:
  val Supply: Int = 2

  /** "Sent {Red}'s pawn to {site}; they lost {2 Supply}." */
  def sentKey(notes: NoteSet): NoteKey = notes.key("sent", Vector(
    NotePart.Text("Sent "), NotePart.Arg(0), NotePart.Text("'s pawn to "),
    NotePart.Arg(1), NotePart.Text("; they lost "), NotePart.Arg(2),
    NotePart.Text(".")))

  /** The loser's pawn placed at `site` and their Supply spent, then the
    * line, read where the walk reaches it. */
  def steps(power: PowerId, card: PowerSourceRef, key: NoteKey,
      loser: PlayerId, site: SiteId): Vector[Operation] = Vector(
    BuildOps((ready, _) => Right(
      PowerAccess.pawnSite(ready, loser).filter(_ != site).toVector
        .map[CoreOperation](from => Move(Piece.Pawn(loser),
          PositionedLocation(Location.Site(from)),
          PositionedLocation(Location.Site(site)))) ++
        Option.when(supplyOf(ready, loser) > 0)(
          SpendSupply(loser, Supply, required = false)))),
    Note(power, states => for
      (before, after) <- states.previous
      lost = supplyOf(before, loser) - supplyOf(after, loser)
    yield key(card, NoteArg.Player(loser), NoteArg.Site(site),
      NoteArg.Amount(lost, NoteUnit.Supply)), covers = true))

  private def supplyOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.players.find(_.player == player)
      .fold(0)(_.board.supply.supply)

/** Spiral Castle (E23, intact), an attacker's battle plan: "If you're
  * victorious in a raid, place the defender's pawn on this site, and they
  * lose 2 Supply."
  *
  * It is used by the ruler of the Castle's site, offered in a Raid only,
  * and costs nothing. Once the Raid is won, it replaces the Raid's
  * relocation question, as Keep replaces it: the defender's pawn is placed
  * at the Castle's site instead of where the attacker would send it, and
  * the defender loses 2 Supply. The takes and the consequences stand. When
  * the defender's Keep also replaces the relocation, Keep's "cannot" wins
  * (catalog batch 9, owner question 3): Keep folds after the Castle and
  * keeps the pawn at Keep's site, while the Supply loss stands.
  */
case object SpiralCastle extends BattlePlan:
  val edifice: EdificeId = SpiralCastleCard.id
  val id: PowerId = SpiralCastleCard.intact.power.id
  private val notes = NoteSet(id)
  val sent: NoteKey = SpiralSteps.sentKey(notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def cardRef: DecisionOptionRef = DecisionOptionRef.Edifice(edifice)
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Attacker)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.edifice(edifice, EdificeSide.Intact)
      .filter(_ => context.setup.kind == CampaignKind.Raid)
      .map(source => CampaignPlanOffer(source,
        "Spiral Castle: send the defeated defender here, losing 2 Supply",
        Vector.empty, Vector.empty))

  override def wrapping: Map[PowerWindow, BattlePlan.Wrap] = Map(
    PowerWindow.CampaignRaidRelocation -> ((_, use, children) =>
      (for
        result <- use.result if result.attackerWins
        defender <- Some(result.defender).collect {
          case CampaignDefender.Player(player) => player }
        site <- castleSite(use.ready)
      yield SpiralSteps.steps(id, PowerSourceRef.Card(edifice), sent,
        defender, site)).getOrElse(children)))

  private def castleSite(ready: ReadyGame): Option[SiteId] =
    ready.game.current.map.sites.collectFirst:
      case (site, state) if state.denizens.exists {
        case card: EdificeState =>
          card.id == edifice && card.side == EdificeSide.Intact
        case _ => false
      } => site

/** Spiral Dungeons (E23, ruined), a defender's battle plan: "If you're
  * victorious, place the attacker's pawn on this site, and they lose 2
  * Supply. Bandits will use this."
  *
  * It is used by the ruler of the Dungeons' site and costs nothing, so a
  * bandit defender applies it when the Dungeons stand at a site bandits
  * rule, as Battle Axes is applied. After the outcome, when the defender
  * won, the attacker's pawn is placed at the Dungeons' site and they lose
  * 2 Supply, in a Conquest or a Raid (owner question 4).
  */
case object SpiralDungeons extends BattlePlan:
  val edifice: EdificeId = SpiralCastleCard.id
  val id: PowerId = SpiralCastleCard.ruined.power.id
  private val notes = NoteSet(id)
  val sent: NoteKey = SpiralSteps.sentKey(notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def cardRef: DecisionOptionRef = DecisionOptionRef.Edifice(edifice)
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.edifice(edifice, EdificeSide.Ruined).map(source =>
      CampaignPlanOffer(source,
        "Spiral Dungeons: send a defeated attacker here, losing 2 Supply",
        Vector.empty, Vector.empty))

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignActionEligibility -> (use =>
      if !use.won.contains(true) then Vector.empty
      else dungeonsSite(use.ready).toVector.flatMap(site =>
        SpiralSteps.steps(id, PowerSourceRef.Card(edifice), sent, use.actor,
          site))))

  private def dungeonsSite(ready: ReadyGame): Option[SiteId] =
    ready.game.current.map.sites.collectFirst:
      case (site, state) if state.denizens.exists {
        case card: EdificeState =>
          card.id == edifice && card.side == EdificeSide.Ruined
        case _ => false
      } => site
