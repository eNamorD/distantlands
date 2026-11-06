package oathdigital.gameplay.powers.targeting

import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.actions.BannerRules
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  OptionRestriction, PowerCtx}
import oathdigital.gameplay.powers.setup.{HallOfDebateCard,
  TomeGuardiansCard}
import oathdigital.model._

/** A persistent rule of an intact edifice face that keeps a banner its
  * ruler holds from being targeted by the ruler's enemies (catalog batch 9,
  * slice 2): Hall of Debate for the People's Favor, Tome Guardians for the
  * Darkest Secret. The ruler is the ruler of the site the intact face
  * stands at; under bandits or unruled nobody holds the banner through the
  * edifice, so nothing is hidden. Empire rulers are not supported.
  *
  * "Target" means a Campaign's target selection only (catalog batch 3
  * rulings, "Target protections corrected"), so it hides the banner at a
  * Raid's optional targets (`CampaignTargetSelection`), as the Circlet of
  * Command does, and leaves a Challenge and a played Conspiracy alone. The
  * hidden option carries "{Blue}'s People's Favor cannot be targeted." as
  * its reason, and no line is written.
  */
sealed abstract class BannerGuardian extends ContributingPower:
  def edifice: EdificeId
  def banner: Banner
  protected def shielded: NoteKey

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.CampaignTargetSelection ->
      Vector(OptionRestriction(guard, (ctx, _) => protectedRuler(ctx).map(
        ruler => shielded(PowerSourceRef.Card(edifice), NoteArg.Player(ruler),
          NoteArg.Banner(banner))))))

  private def guard(ctx: PowerCtx, ref: DecisionOptionRef)
      : Option[OathViolation] = ref match
    case DecisionOptionRef.Banner(named)
        if named == banner && protectedRuler(ctx).nonEmpty =>
      Some(OathViolation.InvalidEventOrder(
        s"${id.value} protects its ruler's ${BannerRules.displayName(banner)}"))
    case _ => None

  /** The face's ruler, when they hold the banner and the acting player is
    * their enemy. */
  private def protectedRuler(ctx: PowerCtx): Option[PlayerId] = for
    site <- ctx.state.game.current.edificeSite(edifice, EdificeSide.Intact)
    ruler <- SiteRulers.rulerOf(ctx.state, site)
    owner <- Some(ruler).collect { case SiteRuler.Player(owner) => owner }
    if SiteRule.enemies(ruler, SiteRuler.Player(ctx.actor))
    if BannerRules.holder(ctx.state.game.current, banner).contains(owner)
  yield owner

/** Hall of Debate (E01, intact): "If this card's ruler holds the People's
  * Favor, their enemies cannot target the People's Favor." */
case object HallOfDebate extends BannerGuardian:
  val edifice: EdificeId = HallOfDebateCard.id
  val id: PowerId = HallOfDebateCard.intact.power.id
  val banner: Banner = Banner.PeoplesFavor
  private val notes = NoteSet(id)
  /** "{Blue}'s {People's Favor} cannot be targeted." */
  val shielded: NoteKey = BannerGuardian.shieldedKey(notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

/** Tome Guardians (E18, intact): "If this card's ruler holds the Darkest
  * Secret, their enemies cannot target the Darkest Secret." */
case object TomeGuardians extends BannerGuardian:
  val edifice: EdificeId = TomeGuardiansCard.id
  val id: PowerId = TomeGuardiansCard.intact.power.id
  val banner: Banner = Banner.DarkestSecret
  private val notes = NoteSet(id)
  /** "{Blue}'s {Darkest Secret} cannot be targeted." */
  val shielded: NoteKey = BannerGuardian.shieldedKey(notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

object BannerGuardian:
  /** "{Blue}'s {People's Favor} cannot be targeted.": the hidden option's
    * reason, declared by each guardian in its own set. */
  def shieldedKey(notes: NoteSet): NoteKey = notes.key("shielded", Vector(
    NotePart.Arg(0), NotePart.Text("'s "), NotePart.Arg(1),
    NotePart.Text(" cannot be targeted.")))
