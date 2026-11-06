package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.actions.campaign.CampaignProcedure
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.roles.{Citizenship, CouncilSeat}
import oathdigital.model._

/** The Grand Scepter's battle plan, for either side: "[favor-burnt]
  * [favor-burnt][favor-burnt] If you are Imperial and victorious against a
  * Citizen, you may exile them" (CR p. 34, as the card database words it;
  * ruling R23).
  *
  * Offered to an Imperial user whose enemy is a Citizen; it burns 3 favor.
  * Once the Campaign has resolved and the user won, the user chooses
  * whether to exile the Citizen. Roles are read again then, since one may
  * have changed during the Campaign. The first-game option does not touch
  * it: it exiles, it makes no one a Citizen.
  *
  * Against a Citizen holding Council Seat faceup, the plan and the exile
  * carry the Seat's warning, and the exile leaves them a Citizen ("Did not
  * exile p2.").
  *
  * Lines: "Exiled p2." and "Did not exile p2." It narrates its question.
  */
case object GrandScepterPlan extends BattlePlan:
  val relic: RelicId = TheGrandScepterCard.id
  val id: PowerId = TheGrandScepterCard.campaign.id
  val decisionId: String =
    CampaignProcedure.decisionPrefix + "grand-scepter.exile"
  val exile: DecisionOptionRef.Button = DecisionOptionRef.Button("exile")
  val spare: DecisionOptionRef.Button = DecisionOptionRef.Button("spare")
  private val notes = NoteSet(id)
  /** "Exiled {0}." */
  val exiled: NoteKey = notes.key("exiled", Vector(NotePart.Text("Exiled "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "Did not exile {0}." */
  val spared: NoteKey = notes.key("spared", Vector(
    NotePart.Text("Did not exile "), NotePart.Arg(0), NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def cardRef: DecisionOptionRef = DecisionOptionRef.Relic(relic)
  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  private def card: PowerSourceRef = PowerSourceRef.Card(relic)

  def plan(context: PlanContext): Option[CampaignPlanOffer] = for
    user <- context.user
    if Imperials.isImperial(context.ready, user)
    enemy <- context.enemy.player
    if citizen(context.ready, enemy)
    source <- context.relic(relic)
  yield CampaignPlanOffer(source,
    "The Grand Scepter: you may exile the Citizen if victorious",
    Vector(CampaignPlanCost.FavorBurnt(3)), Vector.empty,
    warning = Option.when(CouncilSeat.guards(context.ready, enemy))(
      OptionReason.Written(CouncilSeat.warning)))

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] =
    Map(PowerWindow.CampaignActionEligibility -> resolve)

  /** After a won Campaign, the question and the exile. */
  private def resolve(use: PlanUse): Vector[Operation] = (for
    user <- use.user
    result <- use.result
    if use.won.contains(true)
    enemy <- opponent(use.side, result)
  yield Vector[Operation](Branch((live, _) =>
    if !Imperials.isImperial(live, user) || !citizen(live, enemy) then
      Vector.empty
    else ask(live, user, enemy)))).getOrElse(Vector.empty)

  private def ask(ready: ReadyGame, user: PlayerId, enemy: PlayerId)
      : Vector[Operation] =
    Vector(
      Decide(decisionId, user, DecisionQuery.ChooseOne(Vector(
        CouncilSeat.warned(ready, enemy,
          DecisionOption.Button(exile, s"Exile ${enemy.value}")),
        DecisionOption.Button(spare, "Do not exile")),
        heading = Some("The Grand Scepter: you may exile the Citizen"))),
      Branch((_, pending) =>
        if PowerAnswers.one(pending, decisionId).contains(exile) then
          Vector(Citizenship.exile(enemy), Note(id, states => Some(
            if Imperials.role(states.now, enemy).contains(Role.Exile) then
              exiled(card, NoteArg.Player(enemy))
            else spared(card, NoteArg.Player(enemy)))))
        else Vector(Note(id, _ => Some(spared(card, NoteArg.Player(enemy)))))))

  private def citizen(ready: ReadyGame, player: PlayerId): Boolean =
    Imperials.role(ready, player).contains(Role.Citizen)

  /** The player on the other side of the plan's user. */
  private def opponent(side: CampaignPlanSide, result: CampaignResult)
      : Option[PlayerId] = side match
    case CampaignPlanSide.Attacker => result.defender.player
    case CampaignPlanSide.Defender => Some(result.attacker)
