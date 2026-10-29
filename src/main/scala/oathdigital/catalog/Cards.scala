package oathdigital.catalog

import oathdigital.model.{CampaignPlanSide, Cost, DenizenId, EdificeId,
  LegacyId, MajorActionType, PowerId, RelicId, SiteId, Suit, Tokens}

/** The modifier icon a power prints beside its braid, when it prints one:
  * the major actions it modifies, or, for a battle plan, the sides of a
  * Campaign that may play it. A battle plan is a kind of Campaign modifier,
  * so it is a case here rather than a mark of its own, and a plan that also
  * modifies some other action cannot be written.
  */
sealed trait ModifierMark extends Product with Serializable
object ModifierMark:
  final case class Action(actions: MajorActionType*) extends ModifierMark:
    require(actions.nonEmpty && actions.distinct == actions,
      "a modifier names each action it modifies once")
    require(!actions.contains(MajorActionType.Forge),
      "no printed power modifies Forge")
  final case class BattlePlan(sides: CampaignPlanSide*) extends ModifierMark:
    require(sides.nonEmpty && sides.distinct == sides,
      "a battle plan names each side it is played on once")

  val AttackPlan: BattlePlan = BattlePlan(CampaignPlanSide.Attacker)
  val DefensePlan: BattlePlan = BattlePlan(CampaignPlanSide.Defender)
  /** Played on either side: the card prints the attack and defense colours
    * as one gradient. */
  val EitherSidePlan: BattlePlan =
    BattlePlan(CampaignPlanSide.Attacker, CampaignPlanSide.Defender)

/** A power as its card prints it. `cost` is the run of favor and secret
  * symbols the printed text opens with; `text` is the rest, timing keyword
  * included. `persistent` is the black braid; `modifier` the icon beside it,
  * which is printed and so checked against the engine's own powers rather
  * than read from them. Gameplay never reads `text`: behaviour lives in the
  * power.
  */
final case class PrintedPower(id: PowerId, persistent: Boolean, cost: Cost,
    text: String, modifier: Option[ModifierMark] = None):
  require(text.trim.nonEmpty, "printed power text must not be blank")
  require(!persistent || !modifier.exists {
    case _: ModifierMark.BattlePlan => true
    case _ => false }, "no battle plan is printed persistent")

  /** The cost symbols in printed order, one per token: "favor", "secret",
    * "favor-burnt", "secret-burnt". */
  def costSymbols: Vector[String] =
    Vector("favor" -> cost.favor, "secret" -> cost.secret,
      "favor-burnt" -> cost.favorBurnt, "secret-burnt" -> cost.secretBurnt)
      .flatMap((symbol, count) => Vector.fill(count)(symbol))

  /** The printed line: the cost symbols in printed order, then `text`. */
  def rulesText: String =
    val symbols = costSymbols.map(symbol => s"[$symbol]")
    if symbols.isEmpty then text else symbols.mkString("", " ", " ") + text

/** Printed restrictions a card mixes in. A locked adviser-only denizen mixes
  * in both `Locked` and `AdviserOnly`; an edifice's intact face is `Locked`.
  */
trait Locked
trait SiteOnly
trait AdviserOnly

/** A card, or an edifice face, that prints powers. */
trait PrintsPowers:
  def powers: Vector[PrintedPower]
  final def rulesText: String = powers.map(_.rulesText).mkString("\n\n")

abstract class Denizen(val id: DenizenId, val name: String, val suit: Suit)
    extends PrintsPowers

abstract class Relic(val id: RelicId, val name: String, val value: Int,
    val defense: Int) extends PrintsPowers

abstract class EdificeFace(val name: String) extends PrintsPowers

/** An edifice card: one suit, two named faces. A card object implements
  * `intact` and `ruined` with nested face objects. */
abstract class Edifice(val id: EdificeId, val suit: Suit):
  def intact: EdificeFace
  def ruined: EdificeFace

abstract class Legacy(val id: LegacyId, val name: String) extends PrintsPowers

/** A site. It prints no rules text. A site object names each of its
  * power ids as a member and lists them in `handlers`. */
abstract class Site(
    val id: SiteId,
    val name: String,
    val defense: Int,
    val capacity: Int,
    val relicSlots: Int,
    val recoverDifficulty: Option[Int],
    val startingResources: Tokens,
    val forgeRequirements: Option[Tokens],
    val homeland: Option[Suit]
):
  def handlers: Vector[PowerId]
