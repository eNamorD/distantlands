package oathdigital.gameplay.powers.recover

import oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.actions.recover.RecoverProcedure
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Restriction}
import oathdigital.model._

object RelicPatrolsCard extends Edifice(EdificeId("E17"), Suit.Order):
  object intact extends EdificeFace("Relic Patrols") with Locked:
    val power = PrintedPower(PowerId("edifice.e17.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Recover)),
      text = "Enemies of this card's ruler **cannot** recover relics at " +
        "sites ruled by this card's ruler.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Bandit Patrols"):
    val power = PrintedPower(PowerId("edifice.e17.ruined"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Recover)),
      text = "Enemies of this card's ruler **cannot** recover relics at " +
        "sites ruled by bandits.")
    val powers: Vector[PrintedPower] = Vector(power)

/** A face of the Patrols edifice (E17), a persistent rule that refuses a
  * Recover at its start (catalog batch 9, slice 3), as Vow of Peace refuses
  * a Campaign: a `Restriction` at `RecoverActionEligibility`, judged for
  * the Recover's actor at their pawn's site. The Act menu shows Recover
  * unavailable with the face's reason.
  *
  * The ruler is the ruler of the site the face stands at. Every other
  * player is a player ruler's enemy, and every player is an enemy where
  * bandits rule (batch 8, slice 10, and the Tome Guardians FAQ). Nobody is
  * restricted at an unruled site, and Empire rulers are not supported.
  *
  * The refusal is not asked of the ruler: the Act menu has no askable
  * action, and the walk has no decision a grant could be keyed on before
  * the refusal, so consent waits on that seam (catalog batch 9, "Rulings
  * made while implementing"). A Recover another power starts is the same
  * action and is refused the same way.
  */
sealed abstract class PatrolRule extends ContributingPower:
  def edifice: EdificeId
  protected def side: EdificeSide
  /** Whether a Recover at `site`, ruled by `ruler`, is within the face's
    * reach, given the face's own ruler `patrolsRuler`. */
  protected def patrolled(ruler: Option[SiteRuler],
      patrolsRuler: SiteRuler): Boolean
  protected def reason: String

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.RecoverActionEligibility ->
      Vector(Restriction((ctx, _) => blocked(ctx))))

  override def applicable(ctx: PowerCtx): Boolean =
    ctx.state.game.current.edificeSite(edifice, side).isDefined

  private def blocked(ctx: PowerCtx): Option[OathViolation] =
    val refused = for
      patrolsSite <- ctx.state.game.current.edificeSite(edifice, side)
      patrolsRuler <- SiteRulers.rulerOf(ctx.state, patrolsSite)
      if SiteRule.enemies(patrolsRuler, SiteRuler.Player(ctx.actor))
      recoverSite <- RecoverProcedure.actorSite(ctx.state, ctx.actor)
      if patrolled(SiteRulers.rulerOf(ctx.state, recoverSite), patrolsRuler)
    yield OathViolation.StartUnavailable(reason)
    refused

/** Relic Patrols (E17, intact): "Enemies of this card's ruler cannot
  * recover relics at sites ruled by this card's ruler." Under bandits the
  * bandits are its ruler, so every player is refused at every bandit site,
  * as Bandit Patrols refuses them (owner's ruling, catalog batch 9,
  * "Rulings made while implementing"). */
case object RelicPatrols extends PatrolRule:
  val edifice: EdificeId = RelicPatrolsCard.id
  val id: PowerId = RelicPatrolsCard.intact.power.id
  protected def side: EdificeSide = EdificeSide.Intact
  val reason: String = "Relic Patrols: Cannot recover relics at its " +
    "ruler's sites."

  protected def patrolled(ruler: Option[SiteRuler],
      patrolsRuler: SiteRuler): Boolean = ruler.contains(patrolsRuler)

/** Bandit Patrols (E17, ruined): "Enemies of this card's ruler cannot
  * recover relics at sites ruled by bandits." Under bandits every player
  * is refused at every bandit site. */
case object BanditPatrols extends PatrolRule:
  val edifice: EdificeId = RelicPatrolsCard.id
  val id: PowerId = RelicPatrolsCard.ruined.power.id
  protected def side: EdificeSide = EdificeSide.Ruined
  val reason: String = "Bandit Patrols: Cannot recover relics at bandit " +
    "sites."

  protected def patrolled(ruler: Option[SiteRuler],
      patrolsRuler: SiteRuler): Boolean =
    ruler.contains(SiteRuler.Bandits)
