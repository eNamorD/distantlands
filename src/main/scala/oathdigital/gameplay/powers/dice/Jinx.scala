package oathdigital.gameplay.powers.dice

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.SiteRulers
import oathdigital.gameplay.actions.campaign.CampaignIds
import oathdigital.gameplay.actions.dice.{DiceThrow, RerollChoice}
import oathdigital.gameplay.operations.Costs
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferReroll, PowerCtx, PowerUse}
import oathdigital.model._

object JinxCard extends Denizen(DenizenId("68"), "Jinx", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.jinx"),
    persistent = true, cost = Cost(secret = 1),
    text = "If you rule Jinx, after you roll [attack-die] or " +
      "[defense-die] for any reason, you may use this power to " +
      "reroll all those dice once. _(This is not a battle plan!)_")
  val powers: Vector[PrintedPower] = Vector(power)

/** Jinx (card 68): "If you rule Jinx, after you roll [attack-die] or
  * [defense-die] for any reason, you may use this power to reroll all those
  * dice once." It costs one secret, paid onto the card when used.
  *
  * It offers itself at every attack or defense roll step whose roller rules
  * it: a faceup adviser of the roller, or a card faceup at a site the roller
  * rules. A facedown adviser has no power. The roll step stops offering it once it
  * is used on a throw, which is the "once".
  *
  * It writes "Rerolled the attack dice." ("the defense dice"), before the
  * reroll's own roll line.
  */
case object Jinx extends ContributingPower:
  val id: PowerId = JinxCard.power.id
  private val card: DenizenId = JinxCard.id

  /** Its reroll button, after the secret it costs. */
  val label: String = "Reroll all with Jinx"

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Rerolled the attack dice." */
  val rerolledAttack: NoteKey = notes.key("rerolled-attack",
    Vector(NotePart.Text("Rerolled the attack dice.")))
  /** "Rerolled the defense dice." */
  val rerolledDefense: NoteKey = notes.key("rerolled-defense",
    Vector(NotePart.Text("Rerolled the defense dice.")))

  /** Its line for `rolled`; none for the round die, which Jinx's text does
    * not name, so Jinx offers nothing after a round roll. */
  private def line(rolled: DiceThrow): Option[NoteKey] = rolled.die match
    case DiceKind.Attack => Some(rerolledAttack)
    case DiceKind.Defense => Some(rerolledDefense)
    case DiceKind.Round => None

  /** The throw at hand, when its roller rules Jinx and Jinx can reroll it. */
  private def jinxable(ctx: PowerCtx): Option[DiceThrow] =
    RerollChoice.throwOf(ctx).filter(rolled => line(rolled).nonEmpty &&
      Jinx.rules(ctx.state, rolled.roller))

  /** Whether `player` rules Jinx: a faceup adviser of theirs, or faceup at a
    * site they rule. */
  def rules(ready: ReadyGame, player: PlayerId): Boolean =
    ready.game.current.players.find(_.player == player).exists(_.advisers
      .exists {
        case DenizenState(`card`, Orientation.FaceUp, _) => true
        case _ => false
      }) || SiteRulers.rulerOfCard(ready, card)
      .contains(SiteRuler.Player(player))

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.Reroll -> Vector(OfferReroll(offer)))

  /** Its use on the throw at hand (catalog batch 8, slice 10): the
    * roller's, persistent, costing a secret. Nobody is asked about the
    * Campaign's own dice. */
  override def use(ctx: PowerCtx): Option[PowerUse] =
    jinxable(ctx).map(rolled =>
      PowerUse.of(rolled.roller, JinxCard.power, JinxCard.id,
        consent = !Set(CampaignIds.attackPool, CampaignIds.defensePool)
          .contains(rolled.pool)))

  private def offer(ctx: PowerCtx): Option[RerollOffer] =
    jinxable(ctx).map(rolled => RerollOffer(
      JinxCard.id, Jinx.label,
      Some(Costs.onCard(rolled.roller, JinxCard.id, JinxCard.power.cost,
        Some(JinxCard.suit), intoOccupied = true)),
      Vector(Note(id, _ => line(rolled).map(_(
        PowerSourceRef.Card(JinxCard.id)))),
        Reroll(rolled.pool, rolled.indices))))
