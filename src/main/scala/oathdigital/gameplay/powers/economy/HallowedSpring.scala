package oathdigital.gameplay.powers.economy

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  OperationRewrite, PowerCtx, Transform}
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._

object HallowedSpringCard extends Edifice(EdificeId("E26"), Suit.Beast):
  object intact extends EdificeFace("Hallowed Spring") with Locked:
    val power = PrintedPower(PowerId("edifice.e26.intact"),
      persistent = true, cost = Cost.free,
      text = "When trading at this site for [secret], you **must** place " +
        "both [favor] on the card you are trading with _(instead of " +
        "burning one)_.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Hiding Place"):
    val power = PrintedPower(PowerId("edifice.e26.ruined"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (THRONE): This site's ruler may secretly keep a " +
        "relic they have facedown. They start the next game with it.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Hallowed Spring (E26, intact), a persistent rule: "When trading at this
  * site for secrets, you must place both favor on the card you are trading
  * with (instead of burning one)." (catalog batch 9, slice 5)
  *
  * A `Transform` at `TradeCost` rewrites the Trade's payment: a `PayCost`
  * of one placed and one burnt favor onto a card standing at the Spring's
  * site becomes two placed favor. It is a must, binds every player, the
  * site's ruler included, and asks nothing. A Trade for favor pays a
  * secret and is left alone, as is a Trade with a card elsewhere. It
  * writes no line: the payment's generic line shows the 2 favor placed.
  * The ruined face, Hiding Place, is a Chronicle step and is not
  * implemented.
  */
case object HallowedSpring extends ContributingPower:
  val edifice: EdificeId = HallowedSpringCard.id
  val id: PowerId = HallowedSpringCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** The Trade for secrets' printed payment, and what it becomes here. */
  val printed: Cost = Cost(favor = 1, favorBurnt = 1)
  val placed: Cost = Cost(favor = 2)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TradeCost -> Vector(Transform((ctx, children) =>
      OperationRewrite.ops(ctx, children)((_, _) => {
        case pay @ PayCost(_, Location.OnCard(card), `printed`, _, _, _)
            if atSpring(ctx.state, card) =>
          Vector(pay.copy(cost = placed))
      }))))

  override def applicable(ctx: PowerCtx): Boolean =
    springSite(ctx.state).isDefined

  def springSite(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, edifice, EdificeSide.Intact)

  /** Whether `card` stands at the Spring's site. */
  private def atSpring(ready: ReadyGame, card: CardId): Boolean =
    springSite(ready).flatMap(ready.game.current.map.sites.get)
      .exists(_.denizens.exists(_.id == card))
