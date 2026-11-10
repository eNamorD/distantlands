package distantlands.application

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.{SearchFixture, WalkerPowerCatalog}
import distantlands.gameplay.powers.action.AncientBindingCard
import distantlands.gameplay.powers.campaign.{RainBootsCard, SealingWardCard}
import distantlands.gameplay.SacredGroundCard
import distantlands.gameplay.powers.cardplay.{SacredGround, VisionPlayFixture,
  VowOfObedience, VowOfObedienceCard}
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.DecisionOptionProjection
import distantlands.testkit.{Outcomes, PlacementPicks, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.{accepted, present}

/** Why a card play option is not on offer, from the engine to the projected
  * row (card play slots design, "Rulings"): a power's note for the faceup
  * plays it forbids, a printed protection's words for a discard it refuses.
  */
class CardPlayReasonsSuite extends munit.FunSuite:
  private val projector = new WalkerDecisionProjector(catalog,
    new GamePresentationProjector(catalog))
  private val powers = WalkerPowerCatalog.default(catalog)
  private val spot = DecisionOptionRef.RevealedVision(p1)

  /** The rows of the decision p1 is shown at `state`. */
  private def rows(state: OathState)(using munit.Location)
      : Vector[DecisionOptionProjection] =
    present(projector.project(ScopedProjectionContext(Outcomes.ready(state),
      Some(p1))), "the projected decision").query.toVector
      .flatMap(_.offeredOptions)

  /** A Search by p1 that drew Faith, parked on its placement, on the table
    * `arrange` sets. */
  private def searchedFaith(arrange: Table => Table)(
      using munit.Location): OathTransition =
    VisionPlayFixture.searched(arrange(Table.from(
      SearchFixture.staged(Vector(VisionRules.Faith), supply = 5))).ready,
      VisionRules.Faith)

  /** The power whose note hid the revealed Vision spot at `state`. */
  private def hiddenBy(state: OathState)(using munit.Location)
      : Option[PowerId] =
    PlacementPicks.parkedOptions(state, powers).collectFirst {
      case DecisionOption.Unavailable(option, OptionReason.Noted(power, _))
          if option.ref == spot => power
    }

  private def assertSpotRefused(state: OathState)(using munit.Location): Unit =
    val shown = present(rows(state).find(_.kind == spot.kind),
      "the revealed Vision row")
    assertEquals(shown.state, "unavailable")
    assert(shown.reason.exists(_.endsWith("Cannot play a Vision faceup.")),
      shown.reason.toString)

  test("a Vow of Obedience holder sees the revealed Vision spot refused, " +
      "with the Vow's reason"):
    val parked = searchedFaith(_.adviser(p1,
      VowOfObedienceCard.id))
    assertEquals(hiddenBy(parked.state), Some(VowOfObedience.id))
    assertSpotRefused(parked.state)

  test("a player away from Sacred Ground sees the revealed Vision spot " +
      "refused, with its reason"):
    val parked = searchedFaith(_.edifice(SacredGroundCard.id,
      EdificeSide.Intact, at = BrokenPeaksSite.id))
    assertEquals(hiddenBy(parked.state), Some(SacredGround.id))
    assertSpotRefused(parked.state)

  test("a faceup locked adviser's Replace rows say Locked"):
    val sealingWard = SealingWardCard.id
    val errandBoy = ErrandBoyCard.id
    // Three advisers fill p1's adviser limit (3 by default), so keeping the
    // drawn Errand Boy means replacing one of them: Replace rows exist.
    val ready = Vector(RainBootsCard, AncientBindingCard).foldLeft(Table.from(
      SearchFixture.staged(Vector(errandBoy), supply = 5)))(
      _.adviser(p1, _, facedown = true)).adviser(p1, sealingWard).ready
    // The Search may draw more than one card: keep Errand Boy.
    val parked = accepted(SearchFixture.start(ready)
      .flatMap(SearchFixture.keep(_, errandBoy)))
    val replacing = rows(parked.state).filter(row =>
      row.kind == "adviser-play" &&
        row.replaces.exists(_.cardId == sealingWard.value))
    assertEquals(replacing.map(row => (row.state, row.reason)).distinct,
      Vector(("unavailable", Some("Locked: cannot be discarded"))))
