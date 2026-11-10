package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower, SiteOnly}
import oathdigital.gameplay.actions.campaign.CampaignProcedure
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.roles.{Citizenship, RoleNotes}
import oathdigital.model._

object MartialCultureCard extends Denizen(DenizenId("10"), "Martial Culture", Suit.Order) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.martial-culture"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.EitherSidePlan),
    text = "If you're an Exile and defeat another Exile, you may become " +
      "a Citizen. _Follow the steps in the rules and end your Act " +
      "Phase._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Martial Culture (card 10, site-only), a battle plan for either side: "If
  * you're an Exile and defeat another Exile, you may become a Citizen"
  * (ruling R23).
  *
  * Offered to an Exile user whose enemy is an Exile player, never bandits,
  * and shown withheld under the first-game option. Once the user has won,
  * they choose whether to become a Citizen, or the Chancellor in a game
  * with no Empire. A defender who wins off-turn ends no Act phase.
  *
  * Lines: "Became a Citizen.", "Became the Chancellor." and "Declined
  * Citizenship." It narrates its question.
  */
case object MartialCulture extends DenizenPlan(MartialCultureCard,
    MartialCultureCard.power):
  val decisionId: String =
    CampaignProcedure.decisionPrefix + "martial-culture"
  val become: DecisionOptionRef.Button = DecisionOptionRef.Button("become")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  private val notes = NoteSet(id)
  val roleNotes: RoleNotes = RoleNotes.declare(notes)
  /** "Declined Citizenship." */
  val declined: NoteKey = notes.key("declined",
    Vector(NotePart.Text("Declined Citizenship.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def sides: Set[CampaignPlanSide] =
    Set(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

  private def cardSource: PowerSourceRef = PowerSourceRef.Card(cardId)

  def plan(context: PlanContext): Option[CampaignPlanOffer] = for
    user <- context.user
    if exile(context.ready, user)
    enemy <- context.enemy.player
    if exile(context.ready, enemy)
    source <- context.denizen(cardId)
  yield CampaignPlanOffer(source,
    "Martial Culture: you may become a Citizen if victorious",
    Vector.empty, Vector.empty,
    withheld = Option.when(Citizenship.ignored(context.ready))(
      OptionReason.Written(Citizenship.ignoredReason)))

  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] =
    Map(PowerWindow.CampaignActionEligibility -> resolve)

  /** After a won Campaign, the question and the role change. Only the
    * user's role is read again: the card asks whether "you're an Exile",
    * and the defeated Exile was one when the plan was chosen. */
  private def resolve(use: PlanUse): Vector[Operation] = (for
    user <- use.user
    if use.won.contains(true)
  yield Vector[Operation](Branch((live, _) =>
    if exile(live, user) then ask(user) else Vector.empty)))
    .getOrElse(Vector.empty)

  private def ask(user: PlayerId): Vector[Operation] = Vector(
    Decide(decisionId, user, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(become, "Become a Citizen"),
      DecisionOption.Button(decline, "Stay an Exile")),
      heading = Some("Martial Culture: you may become a Citizen"))),
    Branch((_, pending) =>
      if PowerAnswers.one(pending, decisionId).contains(become) then
        Vector(Citizenship.become(user),
          Note(id, roleNotes.became(_, user, cardSource)))
      else Vector(Note(id, _ => Some(declined(cardSource))))))

  private def exile(ready: ReadyGame, player: PlayerId): Boolean =
    Imperials.role(ready, player).contains(Role.Exile)
