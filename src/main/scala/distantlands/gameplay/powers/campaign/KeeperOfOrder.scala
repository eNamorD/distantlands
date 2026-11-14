package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Legacy, LegacyGoal, ModifierMark, PrintedPower}
import distantlands.gameplay.actions.campaign.{CampaignProcedure, PlanAmounts}
import distantlands.model._

object KeeperOfOrderCard extends Legacy(LegacyId("L11"), "Keeper of Order",
    LegacyGoal.GrandScepter, Suit.Order):
  val power = PrintedPower(PowerId("legacy.keeper-of-order"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "**IF YOU ARE AN IMPERIAL:** **BATTLE PLAN:** If your enemy " +
      "is an Imperial, ±2 [attack-die] and you ignore all [skull] " +
      "you roll when attacking.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Keeper of Order (legacy L11): "If you are an Imperial: Battle plan: If
  * your enemy is an Imperial, ±2 attack dice and you ignore all skulls you
  * roll when attacking" (Legacy in-play powers design, ruling 16).
  *
  * A plan for either side, offered while its user is an Imperial and the
  * enemy an Imperial player, as in Imperial Infighting; bandits are never
  * an Imperial. Its user is the holder, or another Imperial it is lent to
  * through Imperial Tactics (ruling 5). Once picked, its user chooses to
  * add 2 attack dice or remove 2 (implementation ruling R7), whichever
  * side they are on. When its user attacks, every skull they roll is
  * ignored (`AttackScore.changes`), rescored as Outriders rescores. It is
  * used once per Campaign, as every plan is.
  *
  * Lines (R8): "Added 2 attack dice." or "Removed 2 attack dice.", and
  * Outriders' "Skulls ignored." when it ignores a skull.
  */
case object KeeperOfOrder extends BattlePlan:
  val id: PowerId = KeeperOfOrderCard.power.id
  val Dice: Int = 2
  /** Under the Campaign's prefix, so a parked question is a Campaign
    * decision. */
  val decisionId: String = CampaignProcedure.decisionPrefix +
    "keeper-of-order"
  def cardRef: DecisionOptionRef =
    DecisionOptionRef.Legacy(KeeperOfOrderCard.id)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  private val notes = NoteSet(id)
  notes.include(Outriders.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Added 2 attack dice." */
  val added: NoteKey = notes.key("added", Vector(NotePart.Text("Added "),
    NotePart.Arg(0), NotePart.Plural(0, " attack die.", " attack dice.")))
  /** "Removed 2 attack dice." */
  val removed: NoteKey = notes.key("removed", Vector(
    NotePart.Text("Removed "), NotePart.Arg(0),
    NotePart.Plural(0, " attack die.", " attack dice.")))

  private def card: PowerSourceRef = PowerSourceRef.Card(KeeperOfOrderCard.id)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    def imperial(player: PlayerId) =
      Imperials.isImperial(context.ready, player)
    val enemyImperial = context.enemy match
      case CampaignDefender.Player(enemy) => imperial(enemy)
      case CampaignDefender.Bandits => false
    for
      user <- context.user
      if imperial(user) && enemyImperial
      held <- context.legacy(KeeperOfOrderCard.id)
    yield CampaignPlanOffer(held,
      s"Keeper of Order: add or remove $Dice attack dice", Vector.empty,
      Vector.empty, amount = Some(PlanAmounts.oneOf(decisionId,
        s"Keeper of Order: add or remove $Dice attack dice?", Vector(
          Dice -> s"Add $Dice attack dice",
          -Dice -> s"Remove $Dice attack dice"))(signed => Vector(
        if signed > 0 then CampaignPlanEffect.AddAttackDice(Dice)
        else CampaignPlanEffect.RemoveAttackDice(Dice),
        CampaignPlanEffect.Run(Vector(Note(id, _ => Some(
          (if signed > 0 then added else removed)(card,
            NoteArg.Number(Dice))))))))))

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignAttackResult -> (use =>
      if use.side == CampaignPlanSide.Attacker then
        Outriders.ignoreSkulls(use, id, card)
      else Vector.empty))
