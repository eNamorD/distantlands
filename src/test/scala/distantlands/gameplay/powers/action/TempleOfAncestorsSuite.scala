package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{NoteText, PhasePowerCatalog,
  PowerFixture, WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Look, PlacementPicks, PowerRules, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Temple of Ancestors (catalog batch 9, slice 9): draw the world deck's
  * bottom card and play it to the advisers, or discard it, as if searched. */
class TempleOfAncestorsSuite extends munit.FunSuite:
  import PowerFixture._
  import distantlands.testkit.PhasePowerHarness.{after, awaits, parked}

  private val temple = TempleOfAncestorsCard.id
  private val source = DecisionOptionRef.Edifice(temple)
  private val power = TempleOfAncestors
  private val rules = PowerRules.withWalkerAndPhasePowers
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val phasePowers = PhasePowerCatalog.default

  /** p1's Act at Ancient City beside the intact Temple with 2 favor, and
    * `bottom` at the bottom of the world deck. */
  private def staged(bottom: WorldCardId = WrestlersCard.id): ReadyGame =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .edifice(temple, EdificeSide.Intact, at = AncientCitySite)
      .favor(actor, 2).worldDeckBottom(bottom).ready

  private def start(ready: ReadyGame) = rules.startWalker(
    OathState.Ready(ready), ActionRef.UsePower(TempleOfAncestors.id), actor,
    Vector(source))

  private def options(from: OathTransition): Vector[DecisionOption] =
    PlacementPicks.parkedOptions(from.state, walkerPowers, phasePowers)

  private def place(from: OathTransition, card: WorldCardId, way: PlayedAs) =
    rules.resolveWalker(from.state, actor,
      CardPlayProcedure.placementDecisionId(card),
      PlacementPicks.parkedAnswer(from.state, way, walkerPowers, phasePowers))

  test("it pays a favor, draws the bottom card into the hand and offers " +
      "the adviser plays and the discard, the site slot refused"):
    val ready = staged()
    val t = accepted(start(ready))
    assert(awaits(t, CardPlayProcedure.placementDecisionId(WrestlersCard.id)))
    val drew = after(t)
    assertEquals(Look(drew).tokensOn(temple), Tokens(1, 0))
    assertEquals(drew.game.current.temporaryHands.get(actor),
      Some(Vector[WorldCardId](WrestlersCard.id)))
    assert(!drew.game.current.commonCards.worldDeck.contains(WrestlersCard.id))
    assertEquals(PlacementPicks.offered(options(t)).toSet,
      Set(PlayedAs.Discarded, PlayedAs.Faceup, PlayedAs.Facedown))
    val site = PlacementPicks.siteSlotOptions(options(t))
    assert(site.nonEmpty)
    site.foreach(slot => assertEquals(slot match {
      case DecisionOption.Unavailable(_, reason) => Some(reason)
      case _ => None
    }, Some(OptionReason.Written(TempleOfAncestors.toAdvisers))))
    assertEquals(NoteText.said(power, t.events), Vector(NoteText.Said(
      NoteKey.Used, s"${actor.value} drew ${WrestlersCard.id.value} from " +
        "the bottom of the world deck.", covers = false)))

  test("played faceup it becomes an adviser; replay and the wire agree"):
    val ready = staged()
    val t = accepted(start(ready))
    val done = accepted(place(t, WrestlersCard.id, PlayedAs.Faceup))
    parked.assertResumed(done.state, Phase.Act, actor)
    assertEquals(Look(after(done)).advisers(actor),
      Vector[CardId](WrestlersCard.id))
    val events = t.events ++ done.events
    assertEquals(distantlands.testkit.ActPowerHarness.replayed(rules, ready,
      events), after(done))
    assertWireRoundTrips(events)

  test("discarded, the card goes to the next region's pile"):
    val t = accepted(start(staged()))
    val done = accepted(place(t, WrestlersCard.id, PlayedAs.Discarded))
    parked.assertResumed(done.state, Phase.Act, actor)
    assert(Look(after(done)).inRegionalDiscard(WrestlersCard.id))

  test("a Vision at the bottom advances the Visions Drawn track and may be " +
      "revealed or discarded"):
    val ready = staged(VisionRules.Faith)
    val t = accepted(start(ready))
    assertEquals(after(t).game.current.tracks.visionsDrawn,
      ready.game.current.tracks.visionsDrawn + 1)
    assertEquals(PlacementPicks.offered(options(t)).toSet,
      Set(PlayedAs.Discarded, PlayedAs.Faceup, PlayedAs.Facedown))

  test("an empty deck draws nothing and says so"):
    val empty = staged().updateCurrent(c => c.copy(commonCards =
      c.commonCards.copy(worldDeck = Vector.empty)))
    val t = accepted(start(empty))
    parked.assertResumed(t.state, Phase.Act, actor)
    assertEquals(NoteText.said(power, t.events), Vector(NoteText.Said(
      "used.none", "The world deck was empty.", covers = false)))
