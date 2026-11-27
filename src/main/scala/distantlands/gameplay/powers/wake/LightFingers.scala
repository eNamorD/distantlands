package distantlands.gameplay.powers.wake

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Restriction}
import distantlands.model._

object LightFingersCard extends Legacy(LegacyId("L27"), "Light Fingers",
    LegacyGoal.HighestRelic, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.light-fingers"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** When you Take Wealth, you may take " +
      "[favor]/[secret] from your site even if enemy pawns are " +
      "there.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Light Fingers (legacy L27): "Wake: When you Take Wealth, you may take
  * favor or secrets from your site even if enemy pawns are there" (Legacy
  * in-play powers design, ruling 17). Its holder's Take Wealth ignores
  * `TakeWealthPawnGate`, always and with no question; it hooks the gate's
  * window only to cast that ignore. It writes no line (implementation
  * ruling R8). */
case object LightFingers extends ContributingPower:
  val id: PowerId = LightFingersCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] =
    Map(PowerWindow.WakeTakeWealth -> Vector(Restriction((_, _) => None)))

  override def applicable(ctx: PowerCtx): Boolean =
    ChronicleLegacyRules.holder(ctx.state, LightFingersCard.id)
      .contains(ctx.actor)

  override def shouldIgnore(other: ContributingPower): Boolean =
    other == TakeWealthPawnGate
