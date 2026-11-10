package distantlands.gameplay.powers.campaign

import distantlands.catalog.{AdviserOnly, Denizen, ModifierMark, PrintedPower}
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  OptionRestriction, PowerCtx}
import distantlands.model._

object BeastTamerCard extends Denizen(DenizenId("90"), "Beast Tamer", Suit.Discord) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.beast-tamer"),
    persistent = true, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Campaign)),
    text = "Enemies cannot use [suit-beast] or [suit-nomad] battle " +
      "plans against you.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Beast Tamer (card 90, adviser-only), a persistent rule of a faceup
  * adviser: "Enemies cannot use [beast] or [nomad] battle plans against
  * you."
  *
  * While its holder is the attacker or the defender, the enemy's Beast and
  * Nomad plans, bandits' included, are withheld with "Beast Tamer: No Beast
  * or Nomad battle plans.". A plan's suit is its source card's
  * (`EnemyPlans.suitOf`); a relic and the title have none.
  */
case object BeastTamer extends ContributingPower:
  val id: PowerId = BeastTamerCard.power.id
  /** The suits of the plans it withholds. */
  val Suits: Set[Suit] = Set(Suit.Beast, Suit.Nomad)
  /** "No Beast or Nomad battle plans." */
  private val notes = NoteSet(id)
  val tamed: NoteKey = notes.key("tamed",
    Vector(NotePart.Text("No Beast or Nomad battle plans.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  val cardId: DenizenId = BeastTamerCard.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  private val barring: OptionRestriction = OptionRestriction(
    (ctx, ref) => Option.when(bars(ctx, ref))(
      OathViolation.CampaignUnavailable(
        "Beast Tamer keeps its holder's enemy from Beast and Nomad plans")),
    (ctx, ref) => Option.when(bars(ctx, ref))(
      BeastTamer.tamed(PowerSourceRef.Card(cardId))))

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.CampaignAttackerBattlePlans -> Vector(barring),
    PowerWindow.CampaignDefenderBattlePlans -> Vector(barring))

  /** Whether `ref` is a Beast or Nomad plan of the holder's enemy. */
  private def bars(ctx: PowerCtx, ref: DecisionOptionRef): Boolean =
    EnemyPlans.holderAgainst(ctx, cardId).nonEmpty &&
      EnemyPlans.suitOf(ctx.catalog, ctx.state, ref).exists(BeastTamer.Suits)
