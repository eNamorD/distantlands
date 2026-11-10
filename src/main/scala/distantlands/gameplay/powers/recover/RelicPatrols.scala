package oathdigital.gameplay.powers.recover

import oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import oathdigital.gameplay.{CardRule, SiteRulers}
import oathdigital.gameplay.actions.recover.RecoverProcedure
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Restriction, Waiver}
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
  * the Recover's actor at their pawn's site.
  *
  * The ruler is the face's ruler (`CardRule`). Every other
  * player is a player ruler's enemy, and every player is an enemy where
  * bandits rule (batch 8, slice 10, and the Tome Guardians FAQ). Nobody is
  * restricted at an unruled site, and Empire rulers are not supported.
  *
  * A player ruler may waive it (catalog batch 10, slice 13). The Act menu
  * shows Recover askable of them, and a grant is keyed on the menu's
  * Recover pick, which the started Recover finds through
  * `WalkerProcedureRegistry.consentAnswers`. Bandits never consent, so
  * under them the menu shows the face's reason, as it does once the ruler
  * refused this turn. A Recover another power starts has no menu pick, so
  * it is refused.
  */
sealed abstract class PatrolRule extends ContributingPower:
  def edifice: EdificeId
  protected def side: EdificeSide
  /** Whether a Recover at `site`, ruled by `ruler`, is within the face's
    * reach, given the face's own ruler `patrolsRuler`. */
  protected def patrolled(ruler: Option[SiteRuler],
      patrolsRuler: SiteRuler): Boolean
  protected def reason: String
  /** The menu's reason while the ruler may be asked. */
  def askedConsent: NoteKey

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** The card the consent prompt and the log name. */
  final def card: PowerSourceRef = PowerSourceRef.Card(edifice)

  /** Who may consent: the face's ruler (`SiteRulers.consentGranterOf`),
    * or the Bandit Crown's holder under bandits; nobody at an unruled
    * site. */
  final def granter(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.edificeSite(edifice, side)
      .flatMap(SiteRulers.consentGranterOf(ready, _, edifice))

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.RecoverActionEligibility -> Vector(Restriction(
      (ctx, _) => blocked(ctx),
      waiver = Some(Waiver(granter, card, (_, _) => askedConsent(card))))))

  override def applicable(ctx: PowerCtx): Boolean =
    ctx.state.game.current.edificeSite(edifice, side).isDefined

  private def blocked(ctx: PowerCtx): Option[OathViolation] =
    val refused = for
      patrolsSite <- ctx.state.game.current.edificeSite(edifice, side)
      patrolsRuler <- CardRule.rulerAt(ctx.state, patrolsSite, edifice)
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
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Cannot recover relics at its ruler's sites without ruler's
    * consent.": the menu's reason while the ruler may be asked. */
  val askedConsent: NoteKey = notes.key("no-recover-consent", Vector(
    NotePart.Text("Cannot recover relics at its ruler's sites without " +
      "ruler's consent.")))

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
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Cannot recover relics at bandit sites without ruler's consent.": the
    * menu's reason while the ruler may be asked. */
  val askedConsent: NoteKey = notes.key("no-recover-consent", Vector(
    NotePart.Text("Cannot recover relics at bandit sites without ruler's " +
      "consent.")))

  protected def patrolled(ruler: Option[SiteRuler],
      patrolsRuler: SiteRuler): Boolean =
    ruler.contains(SiteRuler.Bandits)
