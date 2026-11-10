package distantlands.gameplay.powers.economy

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.{NoteSupport, PowerAccess}
import distantlands.gameplay.powerresolver.{AsksOnSelection, Contribution,
  ModifierPicks,
  OfferReason, PowerCtx, Transform}
import distantlands.gameplay.powers.{PowerAnswers, SelectedModifier}
import distantlands.model._

/** What Small Friends and Friendly Familiar share (catalog batch 8, slice
  * 7): a Trade modifier that, once picked and paid for, asks which site its
  * user acts as if at for the rest of the Trade
  * ([[PowerAccess.asIfDecision]]), and records the answer as state with
  * `ActAsIf` (item 13c).
  *
  * The site then takes the pawn site's place wherever a rule reads where
  * the player acts from ([[PowerAccess.asIfSite]]): later passes of the
  * modifier decision offer its cards' modifiers, City Wall walls the player
  * in there, and the Trade's source options are its cards
  * ([[EconomySources.relocated]]), rewritten before the powers that add
  * cards. The question offers only sites other than the pawn site, and has
  * no Skip.
  *
  * Unavailable, in this order: once the other card acts at another site;
  * once a picked modifier's card stands at the unruled pawn site, since the
  * player is not in two places at once; and with no site to offer. Its line
  * names the site: "Acted as if at {the Shrine}."
  */
trait ActingSite extends SelectedModifier, AsksOnSelection:
  /** The sites the player may act as if at: never the pawn site. */
  def sites(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId)
      : Vector[SiteId]
  /** Why the card is unavailable when `sites` is empty. */
  def noSite: String
  /** The site question's heading. */
  def heading: String

  final def actions: Set[MajorActionType] = Set(MajorActionType.Trade)
  final override def priority: Int = EconomySources.RelocationPriority
  private val notes = NoteSet(id)
  notes.include(ActingSite.notes)
  final override def noteKeys: Vector[NoteKey] = notes.declared
  final override def narratedDecisions: Set[String] =
    Set(PowerAccess.asIfDecision)

  final def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TradeSourceSelection -> Vector(Transform((ctx, operations) =>
      EconomySources.relocated(ctx.catalog, ctx, operations))))

  final override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    val reason =
      if PowerAccess.asIfSite(ctx.state, ctx.actor).nonEmpty then
        Some(ActingSite.elsewhere)
      else if pickedAtPawnSite(ctx) then Some(ActingSite.pickedHere)
      else Option.when(sites(ctx.catalog, ctx.state, ctx.actor).isEmpty)(
        noSite)
    reason.map(OfferReason.Unavailable(_))

  /** The site question, then the answer recorded as state, then the line.
    * The pick and its payment come before the question, so they are judged
    * at the pawn site. */
  final def selectionQuestions(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId): Vector[Operation] = Vector(
    Decide(PowerAccess.asIfDecision, actor, DecisionQuery.ChooseOne(
      sites(catalog, ready, actor).map(site =>
        DecisionOption.Site(DecisionOptionRef.Site(site))),
      heading = Some(heading))),
    BuildOps((_, tree) => Right(PowerAnswers.last(tree.answered,
      PowerAccess.asIfDecision).toVector.collect {
      case DecisionOptionRef.Site(site) => ActAsIf(site)
    })),
    Note(id, actedNote))

  /** Whether a modifier already picked stands at the pawn site, whose
    * cards the actor does not use as a ruler (`PowerAccess.rulingSites`). */
  private def pickedAtPawnSite(ctx: PowerCtx): Boolean =
    val ready = ctx.state
    val picked = ModifierPicks.cards(ctx.answered).toSet
    ready.game.current.pawnSite(ctx.actor)
      .filterNot(PowerAccess.rulingSites(ready, ctx.actor))
      .flatMap(ready.game.current.map.sites.get)
      .exists(_.denizens.exists(card => picked(card.id)))

  private def actedNote(states: NoteStates): Option[LogNote] =
    NoteSupport.answer(states, PowerAccess.asIfDecision).collect {
      case DecisionOptionRef.Site(site) =>
        ActingSite.acted(PowerSourceRef.Card(cardId), NoteArg.Site(site))
    }

object ActingSite:
  /** The keys this object owns, which no one power owns. A power that
    * writes one includes this set. */
  val notes: NoteSet = NoteSet.unowned("ActingSite")
  /** "Acted as if at {the Shrine}." */
  val acted: NoteKey = notes.key("acted", Vector(
    NotePart.Text("Acted as if at "), NotePart.Arg(0), NotePart.Text(".")))
  val elsewhere: String = "Already acting as if at another site"
  val pickedHere: String = "A modifier you picked is at your pawn site"

  /** The sites in play other than `actor`'s pawn site that hold a card
    * `admits`, in map order. */
  def otherSites(ready: ReadyGame, actor: PlayerId)(
      admits: SiteDenizenState => Boolean): Vector[SiteId] =
    val current = ready.game.current
    val pawn = ready.game.current.pawnSite(actor)
    current.map.inPlay.filter(site => !pawn.contains(site) &&
      current.map.sites.get(site).exists(_.denizens.exists(admits)))
