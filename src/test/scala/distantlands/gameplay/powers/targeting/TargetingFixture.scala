package distantlands.gameplay.powers.targeting

import distantlands.gameplay.OathRules
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{ProcedureWalker, WalkerPowers,
  WalkerProcedureRegistry}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{MenuPlay, Outcomes, PowerRules, Table}
import distantlands.testkit.Unwrap.{accepted, present}

/** Shared reading for the suites of the rules that keep a player from being
  * targeted.
  */
object TargetingFixture:
  /** Rules with the walker power catalog, so the printed protections apply. */
  val rules: OathRules = PowerRules.withWalkerPowers
  /** The Fortress edifice: Oaken Fortress intact, Rotting Fortress ruined. */
  val fortress: EdificeId = OakenFortressCard.id
  /** The relic whose holder's banners and relics cannot be targeted. */
  val circlet: RelicId = CircletOfCommandCard.id

  /** p1, in Act at Ancient City, can afford a Challenge (6 favor, 6 faceup
    * and 4 facedown secrets) against p2, who stands there holding the
    * People's Favor with 2 favor. */
  val challengeTable: Table = Table.start
    .pawn(Table.p1, at = AncientCitySite).favor(Table.p1, 6)
    .secrets(Table.p1, faceUp = 6, faceDown = 4)
    .peoplesFavor(Some(Table.p2), favor = 2)
    .pawn(Table.p2, at = AncientCitySite)

  def ready(transition: OathTransition)(using munit.Location): ReadyGame =
    Outcomes.ready(transition.state)

  /** The options of the decision a parked walk of `procedure` is waiting on. */
  def optionsAt(transition: OathTransition, procedure: ProcedureRef)(
      using munit.Location): Vector[DecisionOptionRef] =
    val state = ready(transition)
    val current = state.game.current
    val tree = accepted(WalkerProcedureRegistry.rebuild(procedure, catalog,
      state, current.turn.activePlayer, current.walkerStartArgs))
    val powers = WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
      Vector.empty)
    val pending = present(current.walkerPending, "a parked walker decision")
    present(ProcedureWalker.parkedDecide(state, tree, pending, powers),
      "the parked decision").query match
      case one: DecisionQuery.ChooseOne => one.options.filter(_.available).map(_.ref)
      case many: DecisionQuery.ChooseMany => many.options.filter(_.available).map(_.ref)
      case other => throw new AssertionError(s"unexpected query $other")

  /** Every option of the decision a parked walk of `procedure` is waiting
    * on, the unavailable ones too, with the reason they carry. */
  def optionsIncludingUnavailableAt(transition: OathTransition,
      procedure: ProcedureRef)(using munit.Location): Vector[DecisionOption] =
    val state = ready(transition)
    val current = state.game.current
    val tree = accepted(WalkerProcedureRegistry.rebuild(procedure, catalog,
      state, current.turn.activePlayer, current.walkerStartArgs))
    val powers = WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
      Vector.empty)
    val pending = present(current.walkerPending, "a parked walker decision")
    present(ProcedureWalker.parkedDecide(state, tree, pending, powers),
      "the parked decision").query match
      case one: DecisionQuery.ChooseOne => one.options
      case many: DecisionQuery.ChooseMany => many.options
      case other => throw new AssertionError(s"unexpected query $other")

  def start(state: ReadyGame, procedure: ActionRef, actor: PlayerId)
      : Either[OathViolation, OathTransition] =
    rules.startWalker(Ready(MenuPlay.withoutMenu(state)), procedure, actor)

  /** The decision a started walk is parked on, if any. */
  def parkedOn(transition: OathTransition)(using munit.Location)
      : Option[String] =
    PowerRules.parkedUnderWalkerPowers.parkedDecision(transition.state)
      .map(_.decision)
