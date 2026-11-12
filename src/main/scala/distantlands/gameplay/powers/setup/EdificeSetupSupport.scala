package distantlands.gameplay.powers.setup

import distantlands.gameplay.actions.explore.ExploredSite
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.model._

/** Shared reads for the SETUP/WHEN-EXPLORED edifice powers (E02, E06,
  * E22). Where an edifice stands is `CurrentGameState.edificeSite`.
  */
object EdificeSetupSupport:
  /** Whether a face standing at `site` resolves at the node `ctx` hooks.
    * At Setup it resolves wherever it stands. WHEN EXPLORED "triggers when
    * the card is placed on the map by the Explore action" (rulebook, "Setup
    * / When Explored"), so at an Explore it resolves only at the site that
    * Explore placed, and a face that already stood on the map stays quiet.
    */
  def resolvesAt(ctx: PowerCtx, site: SiteId): Boolean = ctx.operation match
    case ExploredSite(explored) => explored == site
    case _ => true

  /** The player who just placed their pawn, and the site they placed it at.
    *
    * At an Explore it is the explorer, the walk's actor, at the site the
    * Explore placed: "place your pawn on that site".
    *
    * At Setup it is the pawn placement that was just answered, reached at a
    * `PowerWindow.SetupPawnPlaced` fold -- always the LAST entry of
    * `ctx.answered`, since `SetupProcedure`'s tree runs a
    * `BuildOps(placePawn, window = Some(SetupPawnPlaced))` immediately
    * after that player's own pawn `Decide`, with nothing else answered in
    * between. `ctx.actor` is NOT this at Setup: it stays
    * `ready.setup.firstPlayer` for the whole Setup walk (`PowerCtx`'s own
    * doc, "not a parked decision's owner"), so a power that placed the
    * actor from `ctx.actor` would attribute every player's placement to
    * the first player alone.
    */
  def pawnPlacement(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    ctx.operation match
      case ExploredSite(site) => Some(ctx.actor -> site)
      case _ => ctx.answered.lastOption.collect:
        case Answered(_, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Site(site)), by) => by -> site

  /** The keys this object owns, which no one power owns. A power that
    * writes one includes this set. */
  val notes: NoteSet = NoteSet.unowned("EdificeSetupSupport")
  /** "Discarded {cards}.": Broken Forge's relics and Empty Grounds'
    * denizens. */
  val discarded: NoteKey = notes.key("discarded", Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0), NotePart.Text(".")))
