package oathdigital.gameplay.powers.search

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.PowerAccess
import oathdigital.gameplay.actions.{CardPlayRules, DiscardPermission,
  PlaySites}
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powerresolver.{Contribution, Transform}
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

object BaronCard extends Denizen(DenizenId("250"), "Baron", Suit.Order):
  val power = PrintedPower(PowerId("denizen.baron"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may play to any site you rule, and you may discard a " +
      "card from that site first.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Baron (card 250), a selected Search modifier with no cost: "You may play
  * to any site you rule, and you may discard a card from that site first."
  * (catalog batch 8, slice 12).
  *
  * Through the `SearchPlayAdviser` window it opens every site its user
  * rules, and adds a discard permission that holds at those sites, the
  * pawn site included when they rule it. The Play-Facedown-Adviser action
  * selects its modifiers at the Search's window, so it applies there too.
  * The sites are read when the play is planned.
  *
  * Its line, "May discard a card at {site} first.", travels in the
  * permission, and card play writes it for the discard it allowed.
  */
case object Baron extends SelectedModifier:
  val id: PowerId = BaronCard.power.id

  /** Whether `actor` rules the site, on the board the play is planned
    * on. */
  def ruledBy(actor: PlayerId): (ReadyGame, SiteId) => Boolean =
    (ready, site) => PowerAccess.ruledSites(ready, actor).contains(site)

  val card: Denizen = BaronCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Search)
  private val notes = NoteSet(id)
  notes.include(CardPlayRules.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match
        case tree: CardPlayProcedure.PlacementTree =>
          val ruled = Baron.ruledBy(ctx.actor)
          tree.adjust(children)(_.openSites(PlaySites(ruled))
            .permitSiteDiscard(DiscardPermission.lined(id,
              PowerSourceRef.Card(cardId), appliesAt = ruled)))
        case _ => children)))
