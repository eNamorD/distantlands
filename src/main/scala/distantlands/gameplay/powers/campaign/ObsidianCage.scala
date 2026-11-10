package distantlands.gameplay.powers.campaign

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.model._

object ObsidianCageCard extends Relic(RelicId("R18"), "Obsidian Cage", value = 72, defense = 3):
  val campaign = PrintedPower(PowerId("relic.obsidian-cage.campaign"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "If you're victorious, move all unkilled warbands in your " +
      "enemy's force to Obsidian Cage.")
  val action = PrintedPower(PowerId("relic.obsidian-cage.action"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Move any number of warbands from Obsidian Cage " +
      "to any board of the same color.")
  val powers: Vector[PrintedPower] = Vector(campaign, action)

/** Obsidian Cage (relic R18), a battle plan for either side: "If you're
  * victorious, move all unkilled warbands in your enemy's force to
  * Obsidian Cage." The card prints its braid in both colours.
  *
  * Its holder uses it from the faceup relic, for free, and only against a
  * player: bandit warbands are always killed, so none reach the Cage
  * (owner's ruling, 2026-11-03), and against bandits it is shown
  * unavailable with that reason.
  *
  * When its user wins, the enemy's force as the losses left it moves onto
  * the Cage, read as Shield Wall and Sticky Fire read the force they kill
  * (`KillWholeForce`): a Conquest defender's warbands the victory returned
  * to its board, a Raid defender's whole board, an attacker's whole board
  * and its warbands at a queued origin, with the warbands the enemy's
  * Hospital saved, which survive too (owner's ruling, 2026-11-03). The
  * move runs in the losses window, after the losses, so a Conquest's
  * placement follows it. The warbands
  * keep their force on the card (`CurrentGameState.warbandsOnCards`) and
  * count as out of their bank.
  *
  * Its line names the enemy unless they are the Campaign's actor: "Moved 2
  * of Blue's warbands onto Obsidian Cage." or "Moved 2 warbands onto
  * Obsidian Cage."
  */
case object ObsidianCage extends BattlePlan:
  val relic: RelicId = ObsidianCageCard.id
  val id: PowerId = ObsidianCageCard.campaign.id
  val againstBandits: String = "Bandit warbands are always killed, so none " +
    "reach the Cage"
  private val notes = NoteSet(id)
  /** "Moved {n} of {Blue}'s warbands onto Obsidian Cage." */
  val caged: NoteKey = notes.key("caged", Vector(NotePart.Text("Moved "),
    NotePart.Arg(0), NotePart.Text(" of "), NotePart.Arg(1),
    NotePart.Text("'s warbands onto Obsidian Cage.")))
  /** "Moved {n} warbands onto Obsidian Cage.", the actor's own. */
  val cagedOwn: NoteKey = notes.key("caged.own", Vector(
    NotePart.Text("Moved "), NotePart.Arg(0),
    NotePart.Plural(0, " warband onto Obsidian Cage.",
      " warbands onto Obsidian Cage.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  def cardRef: DecisionOptionRef = DecisionOptionRef.Relic(relic)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.relic(relic).map { source =>
      val offer = CampaignPlanOffer(source,
        "Obsidian Cage: if victorious, cage the enemy's surviving force",
        Vector.empty, Vector.empty)
      context.enemy match
        case CampaignDefender.Bandits => offer.copy(
          withheld = Some(OptionReason.Written(againstBandits)))
        case CampaignDefender.Player(_) => offer
    }

  override def wrapping: Map[PowerWindow, BattlePlan.Wrap] = Map(
    PowerWindow.CampaignLosses -> ((ctx, use, losses) => (for
      result <- use.result
      if use.won.contains(true)
      enemy <- enemyOf(use.side, result)
    yield
      val returns = KillWholeForce.returns(use.ready, result)
      losses ++ Vector[Operation](
        BuildOps((ready, _) => cage(ready, use.side, result, returns)),
        Note(id, cagedNote(enemy, ctx.actor), covers = true)))
      .getOrElse(losses)))

  /** The user's enemy, when a player: bandits have no force left to cage. */
  private def enemyOf(side: CampaignPlanSide, result: CampaignResult)
      : Option[PlayerId] = side match
    case CampaignPlanSide.Attacker => result.defender.player
    case CampaignPlanSide.Defender => Some(result.attacker)

  /** The enemy's force, as the losses left it, moved onto the Cage, with
    * the warbands the enemy's Hospital saved. */
  private def cage(ready: ReadyGame, side: CampaignPlanSide,
      result: CampaignResult, returns: KillWholeForce.Returns)
      : Either[OathViolation, Vector[CoreOperation]] =
    val force = side match
      case CampaignPlanSide.Attacker =>
        KillWholeForce.defenderForce(ready, result, returns)
      case CampaignPlanSide.Defender =>
        KillWholeForce.attackerForce(ready, result)
    // The enemy's warbands their Hospital saved survive too, so they join
    // the force on the Cage rather than reach Hospital's site.
    val fromHospital = enemyOf(side, result).toVector.flatMap(player =>
      Hospital.divert(ready, player, Location.OnCard(relic)))
    val diverted = fromHospital.collect {
      case Move(Piece.Warbands(_, count), _, _, _, _) => count }.sum
    force.map(_.map(stack => Move(Piece.Warbands(stack.kind, stack.count),
      PositionedLocation(stack.at),
      PositionedLocation(Location.OnCard(relic))): CoreOperation) ++
      fromHospital ++ Option.when(diverted > 0)(
        ModifyDicePool(FromHospital, diverted)).toVector)

  /** How many warbands the step put on the Cage, whose they are named
    * unless they are the actor's. */
  private def cagedNote(enemy: PlayerId, actor: PlayerId)(states: NoteStates)
      : Option[LogNote] = for
    (before, after) <- states.previous
    moved = onCage(after) - onCage(before)
    if moved > 0
  yield
    val card = PowerSourceRef.Card(relic)
    if enemy == actor then cagedOwn(card, NoteArg.Number(moved))
    else caged(card, NoteArg.Number(moved), NoteArg.Player(enemy))

  /** How many warbands the Cage took from its enemy's Hospital, which
    * Cursed Cauldron must not count as dead. The walker clears it with
    * every pool when the Campaign ends. */
  val FromHospital: PoolKey = PoolKey("campaign.obsidian-cage.from-hospital")

  /** The count of `FromHospital`. */
  def fromHospital(ready: ReadyGame): Int =
    ready.game.current.rollPools.get(FromHospital).fold(0)(_.count)

  private def onCage(ready: ReadyGame): Int =
    ready.game.current.warbandsOnCards.get(relic).fold(0)(_.values.sum)
