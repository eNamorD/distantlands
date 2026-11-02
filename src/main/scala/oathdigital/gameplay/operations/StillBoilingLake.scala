package oathdigital.gameplay.operations

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.model._

object BoilingLakeCard extends Edifice(EdificeId("E24"), Suit.Discord):
  object intact extends EdificeFace("Boiling Lake") with Locked:
    val power = PrintedPower(PowerId("edifice.e24.intact"),
      persistent = false, cost = Cost.free,
      text = "If you travel to this site and do not rule this card, you " +
        "**must** kill 2 warbands on your board if able.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Still Boiling Lake"):
    val power = PrintedPower(PowerId("edifice.e24.ruined"),
      persistent = true, cost = Cost.free,
      text = "Cards **cannot** be played to this site. _This card can " +
        "still be discarded by People's Favor, etc._")
    val powers: Vector[PrintedPower] = Vector(power)

/** Still Boiling Lake (E24, ruined): "Cards cannot be played to this site."
  * (catalog batch 9, slice 5)
  *
  * A printed rule enforced as an operation restriction, beside the Hall of
  * Ministers: every `Play` whose destination is the site the ruined face
  * stands at is refused, whoever plays, so card play's slot options there
  * show unavailable (`CardSlots` words the refusal's code) and a card play
  * another power opens there (New Growth, Baron) is refused the same way.
  * Nobody may waive it ("Cards cannot"). It stops plays only: a card a power
  * moves there (Giant Python) is not played, and the Lake itself may still
  * be discarded. It writes no line. Its intact face, Boiling Lake, is a
  * Travel rule in `gameplay/powers/travel`.
  */
case object StillBoilingLake extends OperationRestriction with PrintedRule:
  val id: PowerId = BoilingLakeCard.ruined.power.id
  val source: PowerSourceRef = PowerSourceRef.Card(BoilingLakeCard.id)
  /** The refusal's code, which card play words for its slots. */
  val code: String = "still-boiling-lake"

  /** The site the ruined face stands at, if it is on the map. */
  def lakeSite(ready: ReadyGame): Option[SiteId] =
    ready.game.current.map.sites.collectFirst:
      case (site, state) if state.denizens.exists {
        case card: EdificeState =>
          card.id == BoilingLakeCard.id && card.side == EdificeSide.Ruined
        case _ => false
      } => site

  override def reason(ready: ReadyGame, operation: CoreOperation)
      : Option[OperationReason] = operation match
    case Play(card, _, Location.Site(site), _, _)
        if lakeSite(ready).contains(site) =>
      Some(OperationReason(code,
        s"${card.value} cannot be played to ${site.value}: Still Boiling " +
          "Lake", OperationReasonKind.Impossible))
    case _ => None
