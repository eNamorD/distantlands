package distantlands.gameplay.powers.setup

import distantlands.gameplay.actions.explore.ExploredSite
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.setup.SetupPawnPlacement
import distantlands.model._

/** Shared reads for the SETUP/WHEN-EXPLORED edifice powers (E02, E06,
  * E22). Where an edifice stands is `CurrentGameState.edificeSite`.
  */
object EdificeSetupSupport:
  /** The option naming `edifice` in the placer's power-order question
    * (catalog batch 13, ruling 4). */
  def option(edifice: EdificeId): DecisionOption =
    DecisionOption.Edifice(DecisionOptionRef.Edifice(edifice))

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
    * At Setup it is the latest pawn placement answered, read at a
    * `PowerWindow.SetupPawnPlaced` fold through `SetupPawnPlacement`: a
    * question a legacy asks inside the window comes after it. `ctx.actor` is NOT this at Setup: it stays
    * `ready.setup.firstPlayer` for the whole Setup walk (`PowerCtx`'s own
    * doc, "not a parked decision's owner"), so a power that placed the
    * actor from `ctx.actor` would attribute every player's placement to
    * the first player alone.
    */
  def pawnPlacement(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    ctx.operation match
      case ExploredSite(site) => Some(ctx.actor -> site)
      // The latest pawn answer, not the last answer: a legacy asks inside
      // the window, and a resumed walk must find the same placement there
      // (Legacy Setup powers R27). A player who stayed off placed nowhere.
      case _ => SetupPawnPlacement.latest(ctx.answered).flatMap(
        (player, site) => site.map(player -> _))

  /** The keys this object owns, which no one power owns. A power that
    * writes one includes this set. */
  val notes: NoteSet = NoteSet.unowned("EdificeSetupSupport")
  /** "Discarded {cards}.": Broken Forge's relics and Empty Grounds'
    * denizens. */
  val discarded: NoteKey = notes.key("discarded", Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0), NotePart.Text(".")))
