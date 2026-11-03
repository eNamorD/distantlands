package oathdigital.gameplay.powers.travel

import oathdigital.gameplay.ModifierPlay
import oathdigital.gameplay.actions.travel.TravelProcedure
import oathdigital.gameplay.powers.PowerFixture
import oathdigital.gameplay.powers.action.WolvesCard
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, Outcomes, PlacementPicks, Table}
import oathdigital.testkit.Table.p2
import oathdigital.testkit.Unwrap.{accepted, present}

/** Forbidden Forest (catalog batch 9, slice 4): enemies of the Forest's
  * ruler cannot travel to its site without a Beast adviser, unless the
  * ruler consents. */
class ForbiddenForestSuite extends munit.FunSuite:
  import PowerFixture.actor
  import TravelFixture._

  private val destination = DecisionOptionRef.Site(coast)
  private val key = ConsentKey(ForbiddenForest.id, actor,
    TravelProcedure.destinationDecisionId, destination)

  /** The Forest on `side` at the coast, which `ruled` settles (p2 rules it
    * by default); the actor at the Cradle's plains. */
  private def forestAtCoast(side: EdificeSide = EdificeSide.Intact,
      ruled: Table => Table = _.warbandsAt(coast, p2, 1)): ReadyGame =
    on(board(supply = 7))(table => ruled(table
      .edifice(ForbiddenForestCard.id, side, at = coast)))

  /** The coast as the parked destination decision offers it. */
  private def coastOffered(ready: ReadyGame)(using munit.Location)
      : DecisionOption =
    val started = accepted(ModifierPlay.started(rules, Ready(ready),
      ActionRef.Travel, actor, PowerWindow.TravelModifierSelection,
      Vector.empty))
    present(PlacementPicks.parkedOptions(started.state, powers(Vector.empty))
      .find(_.ref == destination), "the coast among the destinations")

  test("an enemy without a Beast adviser cannot travel to the Forest's " +
      "site: the destination is askable of the ruler"):
    val ready = forestAtCoast()
    assert(!candidates(ready).contains(coast))
    assertEquals(coastOffered(ready), DecisionOption.Unavailable(
      DecisionOption.Site(destination), OptionReason.Askable(
        ForbiddenForest.id, ForbiddenForest.noTravelConsent(
          ForbiddenForest.card), Vector(ConsentAsk(p2,
          Vector(ForbiddenForest.id))))))
    assert(travel(ready, coast).isLeft)

  test("a faceup Beast adviser opens it, a facedown one does not"):
    val armed = on(forestAtCoast())(_.adviser(actor, WolvesCard.id))
    assert(candidates(armed).contains(coast))
    assertEquals(Look(Outcomes.ready(accepted(travel(armed, coast)).state))
      .pawn(actor), coast)
    val hidden = on(forestAtCoast())(_.adviser(actor, WolvesCard.id,
      facedown = true))
    assert(!candidates(hidden).contains(coast))

  test("the ruler travels freely, and so does anyone to an unruled Forest"):
    assert(candidates(forestAtCoast(ruled = _.warbandsAt(coast, actor, 1)))
      .contains(coast))
    assert(candidates(forestAtCoast(ruled = _.bandits(coast, 0)))
      .contains(coast))

  test("a remembered grant lets the traveller through"):
    val ready = forestAtCoast()
    val granted = ready.copy(consent =
      ConsentState().granting(Vector(key), ConsentTurn.of(ready)))
    assert(candidates(granted).contains(coast))
    assertEquals(Look(Outcomes.ready(accepted(travel(granted, coast)).state))
      .pawn(actor), coast)

  test("under bandits nobody may be asked: the destination shows its " +
      "plain reason"):
    val ready = forestAtCoast(ruled = _.bandits(coast, 1))
    assertEquals(coastOffered(ready), DecisionOption.Unavailable(
      DecisionOption.Site(destination), OptionReason.Noted(ForbiddenForest.id,
        ForbiddenForest.noTravel(ForbiddenForest.card))))

  test("the ruined face restricts nothing"):
    assert(candidates(forestAtCoast(side = EdificeSide.Ruined))
      .contains(coast))
