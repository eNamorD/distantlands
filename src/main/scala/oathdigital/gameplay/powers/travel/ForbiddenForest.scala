package oathdigital.gameplay.powers.travel

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.gameplay.{EffectiveSuit, SiteRulers}
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Restriction, Waiver}
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._

object ForbiddenForestCard extends Edifice(EdificeId("E29"), Suit.Beast):
  object intact extends EdificeFace("Forbidden Forest") with Locked:
    val power = PrintedPower(PowerId("edifice.e29.intact"),
      persistent = true, cost = Cost.free,
      text = "Enemies of this card's ruler **cannot** travel to this site " +
        "unless they have a [suit-beast] adviser.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Bandit Forest"):
    val power = PrintedPower(PowerId("edifice.e29.ruined"),
      persistent = true, cost = Cost.free,
      text = "Imperials **cannot** place [favor]/[secret] on cards ruled " +
        "by bandits or use their non-persistent powers.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Forbidden Forest (E29, intact), a persistent rule: "Enemies of this
  * card's ruler cannot travel to this site unless they have a Beast
  * adviser." (catalog batch 9, slice 4)
  *
  * It follows the Narrow Pass: a `Restriction` at `TravelActionEligibility`
  * that refuses a route arriving at the Forest's site when the traveller is
  * an enemy of the site's ruler and holds no faceup Beast adviser
  * (`EffectiveSuit.count`, so Marriage counts and a Pledge to Discord
  * holder's advisers do not). The look-ahead hides the destination with
  * the reason. A player ruler may waive it, and the grant is keyed on the
  * destination answer, as the Pass's is; under bandits nobody may, and
  * every player is an enemy. Nobody is restricted at an unruled site. A
  * Travel another player sent counts its destination as the traveller's
  * answer (catalog batch 8, slice 15), so its sender may ask the ruler.
  * The ruined face, Bandit Forest, binds Imperials only and is not
  * implemented.
  */
case object ForbiddenForest extends ContributingPower:
  val id: PowerId = ForbiddenForestCard.intact.power.id
  val card: PowerSourceRef = PowerSourceRef.Card(ForbiddenForestCard.id)
  val reason: String = "Forbidden Forest: Cannot travel here without a " +
    "Beast adviser."
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Cannot travel here without a Beast adviser.": the destination's
    * reason when nobody may be asked. */
  val noTravel: NoteKey = notes.key("no-travel", Vector(NotePart.Text(
    "Cannot travel here without a Beast adviser.")))
  /** "... or ruler's consent.": the reason while its ruler may be asked. */
  val noTravelConsent: NoteKey = notes.key("no-travel-consent", Vector(
    NotePart.Text("Cannot travel here without a Beast adviser or ruler's " +
      "consent.")))

  def forestSite(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, ForbiddenForestCard.id,
      EdificeSide.Intact)

  /** The player who rules the Forest's site, who may consent. */
  def granter(ready: ReadyGame): Option[PlayerId] =
    forestSite(ready).flatMap(SiteRulers.rulerOf(ready, _)).collect {
      case SiteRuler.Player(player) => player }

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelActionEligibility -> Vector(Restriction(
      (ctx, _) => blocked(ctx), (_, _) => Some(noTravel(card)),
      Some(Waiver(granter, card, (_, _) => noTravelConsent(card))))))

  override def applicable(ctx: PowerCtx): Boolean =
    forestSite(ctx.state).isDefined

  private def blocked(ctx: PowerCtx): Option[OathViolation] = for
    route <- TravelRoute.pawnMove(ctx.operation)
    site <- forestSite(ctx.state)
    if route.destination == site
    ruler <- SiteRulers.rulerOf(ctx.state, site)
    if SiteRule.enemies(ruler, SiteRuler.Player(route.player))
    if EffectiveSuit.count(ctx.catalog, ctx.state, route.player,
      Suit.Beast) == 0
  yield OathViolation.StartUnavailable(reason)
