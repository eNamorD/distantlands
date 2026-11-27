package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.gameplay.actions.campaign.{CampaignProcedure, PlanAmounts}
import distantlands.model._

object UnstableSummonCard extends Denizen(DenizenId("225"),
    "Unstable Summon", Suit.Discord):
  val power = PrintedPower(PowerId("denizen.unstable-summon"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.DefensePlan),
    text = "If you hold the Darkest Secret, burn X [secret] from it to " +
      "collect X [defense-die].")
  val powers: Vector[PrintedPower] = Vector(power)

/** Unstable Summon (card 225), a defender's battle plan: "If you hold the
  * Darkest Secret, burn X [secret] from it to collect X [defense-die]."
  *
  * It is free. While its user holds the Darkest Secret, as their card
  * powers see it, with a secret on it, they choose X, from one to the
  * secrets on the banner: X secrets burn from the banner, and X defense dice
  * join the defense pool. Otherwise it is shown unavailable. With Yew Staff
  * the secrets burn from the banner wherever it is.
  *
  * It writes "Burned {n secrets} from the Darkest Secret for {n} defense
  * dice."
  */
case object UnstableSummon extends DenizenPlan(UnstableSummonCard,
    UnstableSummonCard.power):
  /** Under the Campaign's prefix, so a parked question is a Campaign
    * decision. */
  val decisionId: String = CampaignProcedure.decisionPrefix +
    "unstable-summon"
  val heading: String = "Unstable Summon: burn how many secrets from the " +
    "Darkest Secret, for a defense die each?"
  val confirm: String = "Burn"
  val noBanner: String = "Needs the Darkest Secret with a secret on it"
  private val notes = NoteSet(id)
  /** "Burned {n secrets} from the {Darkest Secret} for {n} defense dice." */
  val burned: NoteKey = notes.key("burned", Vector(NotePart.Text("Burned "),
    NotePart.Arg(0), NotePart.Text(" from the "), NotePart.Arg(1),
    NotePart.Text(" for "), NotePart.Arg(2),
    NotePart.Plural(2, " defense die.", " defense dice.")))

  def sides: Set[CampaignPlanSide] = Set(CampaignPlanSide.Defender)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def plan(context: PlanContext): Option[CampaignPlanOffer] =
    context.denizen(cardId).map { source =>
      val secrets = context.ready.game.current.banners.darkestSecret.secrets
      val offer = CampaignPlanOffer(source,
        "Unstable Summon: burn secrets for defense dice", Vector.empty,
        Vector.empty)
      if !context.holdsDarkestSecret || secrets < 1 then
        offer.copy(withheld = Some(OptionReason.Written(noBanner)))
      else offer.copy(requires = Vector(PlanNeed.DarkestSecret),
        amount = Some(PlanAmounts.range(decisionId, 1, secrets, heading,
          confirm)(summon(context.user))))
    }

  /** X secrets the defender burns from the banner, X defense dice, and the
    * line. */
  private def summon(defender: Option[PlayerId])(count: Int)
      : Vector[CampaignPlanEffect] = Vector(
    CampaignPlanEffect.Run(Vector(Burn.secrets(count,
      PositionedLocation(Location.OnBanner(Banner.DarkestSecret)),
      defender))),
    CampaignPlanEffect.AddDefenseDice(count),
    CampaignPlanEffect.Run(Vector(Note(id, _ => Some(burned(
      PowerSourceRef.Card(cardId), NoteArg.Amount(count, NoteUnit.Secret),
      NoteArg.Banner(Banner.DarkestSecret), NoteArg.Number(count)))))))
