package oathdigital.gameplay.powers.cardplay

import oathdigital.catalog.{Denizen, PrintedPower, SiteOnly}
import oathdigital.gameplay.powerresolver.{Contribution, PowerCtx, Transform}
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

object WelcomingPartyCard extends Denizen(DenizenId("50"), "Welcoming Party", Suit.Hearth) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.welcoming-party"),
    persistent = false, cost = Cost.free,
    text = "If you play a denizen card that was not a facedown adviser, " +
      "gain [favor] from the [suit-hearth] bank.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Welcoming Party (card 50), a selected Search modifier: if you play a
  * denizen faceup when it is first drawn, gain 1 favor from the Hearth bank.
  *
  * "When first drawn" is the card's origin: it comes straight from the draw,
  * the temporary hand of a Search. It is played faceup when it goes to a site or
  * becomes a faceup adviser. A card placed facedown does not trigger it, and
  * neither does a card that was already a facedown adviser and is played faceup
  * later by the Play-Facedown-Adviser action. A Vision is not a denizen, and a
  * card does not trigger on its own play.
  *
  * The played-card hook does not carry the origin, so the power reads it from
  * the procedure the window is walked for (`PowerCtx.procedure`): a card played
  * by a Search came straight from its draw. The favor is best-effort, so an
  * empty Hearth bank gives nothing.
  */
case object WelcomingParty extends SelectedModifier:
  def id: PowerId = WelcomingPartyCard.power.id
  val Favor: Int = 1

  val card: Denizen = WelcomingPartyCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Search)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ActionCardPlayedFaceup -> Vector(Transform((ctx, children) =>
      children :+ Gain.Favor(ctx.actor, Suit.Hearth,
        WelcomingParty.Favor))))

  override def appliesAt(ctx: PowerCtx): Boolean = ctx.operation match
    case CardPlayedFaceup(card: DenizenId, _) => card != cardId &&
      ctx.procedure.contains(ActionRef.Search)
    case _ => false
