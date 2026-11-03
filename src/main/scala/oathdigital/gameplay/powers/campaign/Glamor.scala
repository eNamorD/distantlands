package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.actions.campaign.{CampaignProcedure, PlanAmounts}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object GlamorCard extends Denizen(DenizenId("207"), "Glamor", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.glamor"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.AttackPlan),
    text = "You may also target [secret] on your enemy's board except " +
      "their last. Add [defense-die] for each targeted. If you're " +
      "victorious, take them.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Glamor (card 207), an attacker's battle plan: "You may also target
  * secrets on your enemy's board except their last. Add a defense die for
  * each targeted. If you're victorious, take them."
  *
  * It is free and offered against a player defender, in a Raid or a
  * Conquest; bandits have no board, so it is never offered against them.
  * Used in the attacker's plan window like any plan (owner's ruling,
  * 2026-11-03), it then asks its user how many of the defender's board
  * secrets to target, from one to all but one, faceup and facedown alike.
  * Each targeted secret adds one defense die. While the defender holds one
  * secret or none, it is shown unavailable.
  *
  * When the attacker wins, they take the targeted secrets: in a Raid in
  * the transfer, after the printed takes and consequences; in a Conquest
  * in the placement window, as Relic Hunter takes its relics. The take is
  * cut to what still leaves the defender one secret, should their board
  * have lost any since. Faceup secrets are taken first, as Sleight of Hand
  * takes them, and each keeps its side. A defeated attacker takes nothing.
  *
  * Lines: "Targeted {2} secrets for {2} defense dice." when the secrets
  * are chosen, and "Took {2 secrets} from {Blue}." when they are taken.
  */
case object Glamor extends BattlePlan:
  val cardId: DenizenId = GlamorCard.id
  val id: PowerId = GlamorCard.power.id
  /** Under the Campaign's prefix, so a parked question is a Campaign
    * decision. */
  val decisionId: String = CampaignProcedure.decisionPrefix + "glamor"
  val heading: String = "Glamor: target how many of the defender's " +
    "secrets, for a defense die each?"
  val confirm: String = "Target"
  val noSecret: String = "The defender has no secret but their last"
  private val notes = NoteSet(id)
  /** "Targeted {n} secrets for {n} defense dice." */
  val targeted: NoteKey = notes.key("targeted", Vector(
    NotePart.Text("Targeted "), NotePart.Arg(0),
    NotePart.Plural(0, " secret for ", " secrets for "),
    NotePart.Arg(1), NotePart.Plural(1, " defense die.", " defense dice.")))
  /** "Took {n secrets} from {Blue}." */
  val took: NoteKey = notes.key("took", Vector(NotePart.Text("Took "),
    NotePart.Arg(0), NotePart.Text(" from "), NotePart.Arg(1),
    NotePart.Text(".")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  def cardRef: DecisionOptionRef = DecisionOptionRef.Denizen(cardId)
  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Attacker)

  /** Every secret on `player`'s board, faceup and facedown. */
  private def secretsOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.players.find(_.player == player)
      .fold(0)(held => held.board.faceUpSecrets + held.board.faceDownSecrets)

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.setup.defender match
      case CampaignDefender.Bandits => None
      case CampaignDefender.Player(defender) =>
        context.denizen(cardId).map { source =>
          val offer = CampaignPlanOffer(source,
            "Glamor: target the defender's secrets", Vector.empty,
            Vector.empty)
          val targetable = secretsOf(context.ready, defender) - 1
          if targetable < 1 then
            offer.copy(withheld = Some(OptionReason.Written(noSecret)))
          else offer.copy(amount = Some(PlanAmounts.range(decisionId, 1,
            targetable, heading, confirm)(dice)))
        }

  /** One defense die per targeted secret, and the line. */
  private def dice(count: Int): Vector[CampaignPlanEffect] = Vector(
    CampaignPlanEffect.AddDefenseDice(count),
    CampaignPlanEffect.Run(Vector(Note(id, _ => Some(targeted(
      PowerSourceRef.Card(cardId), NoteArg.Number(count),
      NoteArg.Number(count)))))))

  private val takeAt: PlanUse => Vector[Operation] = use =>
    (for
      result <- use.result if use.won.contains(true)
      defender <- Some(result.defender).collect {
        case CampaignDefender.Player(player) => player }
    yield Vector[Operation](
      BuildOps((ready, pending) => Right(takes(ready, use.actor, defender,
        pending))),
      Note(id, tookNote(defender), covers = true))).getOrElse(Vector.empty)

  /** After a victory: in a Raid, in the transfer; in a Conquest, in the
    * placement window, which stands on every Conquest victory. */
  override def later: Map[PowerWindow, PlanUse => Vector[Operation]] = Map(
    PowerWindow.CampaignRaidTransfer -> takeAt,
    PowerWindow.CampaignPlacement -> takeAt)

  /** The targeted secrets, cut to leave the defender one, faceup first,
    * each keeping its side. */
  private def takes(ready: ReadyGame, attacker: PlayerId, defender: PlayerId,
      pending: PendingTree): Vector[CoreOperation] =
    val board = ready.game.current.players.find(_.player == defender)
      .map(_.board)
    (for
      held <- board
      targeted <- PowerAnswers.amount(pending, decisionId)
    yield
      val count = targeted min (held.faceUpSecrets + held.faceDownSecrets - 1)
      val up = count min held.faceUpSecrets
      val down = count - up
      Vector((up, SecretSide.FaceUp), (down, SecretSide.FaceDown))
        .filter(_._1 > 0).map[CoreOperation]((amount, side) =>
          Take(Piece.Secrets(amount), attacker, Location.PlayArea(defender),
            Location.PlayArea(attacker), sourceSide = Some(side))))
      .getOrElse(Vector.empty)

  private def tookNote(defender: PlayerId)(states: NoteStates)
      : Option[LogNote] = for
    (before, after) <- states.previous
    lost = secretsOf(before, defender) - secretsOf(after, defender)
    if lost > 0
  yield took(PowerSourceRef.Card(cardId), NoteArg.Amount(lost,
    NoteUnit.Secret), NoteArg.Player(defender))
