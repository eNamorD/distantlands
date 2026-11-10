package distantlands.testkit

import distantlands.catalog.Denizen
import distantlands.gameplay.powerresolver.Contribution
import distantlands.gameplay.powers.SelectedModifier
import distantlands.gameplay.powers.travel.TentsCard
import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._

/** A modifier for an action no card gives one to yet: Forge, Challenge or
  * Campaign. It stands in for the future power those windows are kept for
  * (modifier walker design, ruling 11).
  */
object EmptyWindowModifier:
  /** Stub of SelectedModifier: free, printed on Tents (whose printed power
    * is player-selected), picked for `actions`, with no effect. */
  final case class OnTents(actions: Set[MajorActionType])
      extends SelectedModifier:
    val card: Denizen = TentsCard
    def id: PowerId = PowerId("denizen.tents")
    def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** That modifier for `action`, and no other power. */
  def powersFor(action: MajorActionType): WalkerPowers =
    WalkerPowers(Vector(OnTents(Set(action))))

  /** The decision `state` is parked on, and its first option, rebuilt with
    * `powers`. */
  def parkedWithFirstOption(state: OathState, powers: WalkerPowers)(
      using munit.Location): (String, Option[DecisionOption]) =
    ParkedNode.of(state, FirstGameSetupFixture.catalog, powers) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) =>
        decide.decisionId -> (decide.query match
          case one: DecisionQuery.ChooseOne => one.options.headOption
          case _ => None)
      case other =>
        munit.Assertions.fail(s"expected a parked decision, got $other")
