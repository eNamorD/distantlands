package distantlands.gameplay.powers.targeting

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower, SiteOnly}
import distantlands.gameplay.SiteRulers
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution, OptionRestriction, PowerCtx}
import distantlands.gameplay.powers.CatalogResolution
import distantlands.model._

object ForgottenVaultCard extends Denizen(DenizenId("75"), "Forgotten Vault", Suit.Arcane) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.forgotten-vault"),
    persistent = true, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Campaign)),
    text = "Enemies of Forgotten Vault's ruler **cannot** target relics " +
      "held by its ruler. If ruled by Empire, all Imperials have " +
      "this power.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Forgotten Vault (card 75, site-only), a persistent rule: enemies of the
  * Vault's ruler cannot target relics that ruler holds, as the Circlet of
  * Command protects its holder's relics. The ruler is the ruler of the Vault's
  * site. Ruled by bandits or unruled, it protects nothing, because bandits hold
  * no relics. If the Empire rules it, it protects every Imperial player's
  * relics.
  *
  * "Target" means a Campaign's target selection only (catalog batch 3
  * rulings, "Target protections corrected"). So it restricts a Raid's
  * optional targets (`CampaignTargetSelection`), through an
  * `OptionRestriction` whose hide hook notes each relic it hides. A played
  * Conspiracy may take the ruler's relic.
  *
  * The Game Log posts identical notes once per action.
  */
case object ForgottenVault extends ContributingPower:
  val id: PowerId = ForgottenVaultCard.power.id

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "{ruler}'s relics cannot be targeted." */
  val shielded: NoteKey = notes.key("shielded", Vector(NotePart.Arg(0),
    NotePart.Text("'s relics cannot be targeted.")))

  val cardId: DenizenId = ForgottenVaultCard.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override lazy val resolution: PowerResolution =
    CatalogResolution.of(ForgottenVaultCard.power)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.CampaignTargetSelection -> Vector(OptionRestriction(guard,
      (ctx, ref) => shieldedOwner(ctx, ref).map(said))))

  private def guard(ctx: PowerCtx, ref: DecisionOptionRef)
      : Option[OathViolation] = Option.when(shieldedOwner(ctx, ref).nonEmpty)(
    OathViolation.InvalidEventOrder(
      "the Forgotten Vault protects its ruler's relics"))

  private def said(ruler: PlayerId): LogNote =
    ForgottenVault.shielded(PowerSourceRef.Card(cardId), NoteArg.Player(ruler))

  /** The players the Vault protects from the acting player: its site's
    * ruler, or every Imperial player while the Empire rules it, when they
    * are enemies of the actor. */
  private def owners(ctx: PowerCtx): Set[PlayerId] =
    ctx.state.game.current.faceUpDenizenSite(cardId)
      .fold(Set.empty[PlayerId])(SiteRulers.protectedBy(ctx.state, _,
        cardId))
      .filter(owner =>
      SiteRule.enemies(SiteRuler.Player(owner), SiteRuler.Player(ctx.actor)))

  /** The protected player holding the relic `ref` names. */
  private def shieldedOwner(ctx: PowerCtx, ref: DecisionOptionRef)
      : Option[PlayerId] = owners(ctx).find(owner => ref match
    case DecisionOptionRef.Relic(relic) =>
      ctx.state.game.current.players.exists(p =>
        p.player == owner && p.relics.exists(_.id == relic))
    case DecisionOptionRef.RelicSlot(slotOwner, _) => slotOwner == owner
    case _ => false)
