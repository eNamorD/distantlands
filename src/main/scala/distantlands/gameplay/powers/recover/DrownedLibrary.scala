package distantlands.gameplay.powers.recover

import distantlands.gameplay.actions.recover.{RecoverProcedure,
  UndergroundLibraryCard}
import distantlands.gameplay.operations.Costs
import distantlands.gameplay.powerresolver.{Contribution, OfferReason,
  OnEdificeFace, PowerCtx, PowerUse}
import distantlands.gameplay.powers.{CatalogResolution, SelectedModifier}
import distantlands.model._

/** Drowned Library (E13, ruined), a Recover modifier for 1 secret: "If your
  * pawn is at this site, you may recover the top card of the relic deck
  * instead. Its recover difficulty is 5." (catalog batch 10, slice 8).
  *
  * It is written as Catacombs is, since a selected modifier's card is a
  * denizen or a relic, never an edifice face. The pick places its secret on
  * the edifice, and the pick is the whole effect: it hooks no window. The
  * Recover reads the pick from the walk's answers
  * (`RecoverProcedure.fromRelicDeck`), then needs 5 and offers the relic
  * deck, so the relic stays unseen until recovered; a site with no Recover
  * difficulty of its own becomes recoverable.
  *
  * It is listed only while the ruined face is up (`OnEdificeFace`), and
  * wherever its user may use the edifice. It is unavailable, in this order,
  * away from the user's pawn, and with an empty relic deck. It writes no
  * line: the start line names it, and the Recover's own line names the
  * relic.
  */
case object DrownedLibrary extends OnEdificeFace:
  private val printed = UndergroundLibraryCard.ruined.power
  val id: PowerId = printed.id
  val edifice: EdificeId = UndergroundLibraryCard.id
  val side: EdificeSide = EdificeSide.Ruined
  /** Why it cannot be picked away from the user's pawn. */
  val awayFromLibrary: String = "Your pawn is not at this site"
  /** Why it cannot be picked with no relic left to recover. */
  val emptyRelicDeck: String = "The relic deck is empty"

  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override lazy val resolution: PowerResolution = CatalogResolution.of(printed)
  override def selectionCard: Option[CardId] = Some(edifice)
  override def selectionWindows: Set[PowerWindow] =
    Set(PowerWindow.RecoverModifierSelection)

  /** Its use at its selection window: its pick places one secret on the
    * edifice. */
  override def use(ctx: PowerCtx): Option[PowerUse] =
    SelectedModifier.selectionAction(ctx.window).map(_ =>
      PowerUse.of(ctx.actor, printed, edifice, secret = true, places = true))

  /** At a selection window, for a Recover only; everywhere, while the
    * ruined face is up and the user's pawn stands at it. Stable across the
    * action: the pawn does not move during a Recover. */
  override def applicable(ctx: PowerCtx): Boolean =
    SelectedModifier.selectionAction(ctx.window).forall(
      _ == MajorActionType.Recover) && atLibrary(ctx.state, ctx.actor)

  override def offerReason(ctx: PowerCtx): Option[OfferReason] =
    val reason =
      if !atLibrary(ctx.state, ctx.actor) then Some(awayFromLibrary)
      else Option.when(
        ctx.state.game.current.commonCards.relicDeck.isEmpty)(emptyRelicDeck)
    reason.map(OfferReason.Unavailable(_))

  override def selectionPayments(ready: ReadyGame, actor: PlayerId)
      : Vector[CoreOperation] =
    Vector(Costs.onCard(actor, edifice, Cost(secret = 1),
      Some(UndergroundLibraryCard.suit), intoOccupied = false))

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** Whether the ruined face stands at the user's pawn site, the site the
    * Recover is made at. */
  private def atLibrary(ready: ReadyGame, actor: PlayerId): Boolean =
    ready.game.current.edificeSite(edifice, side).exists(site =>
      RecoverProcedure.actorSite(ready, actor).contains(site))
