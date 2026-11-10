package distantlands.gameplay.powers.travel

import distantlands.gameplay.cards.{AncientCitySite, DeepWoodsSite}
import distantlands.gameplay.powers.PowerFixture
import distantlands.gameplay.powers.action.{BloodPactCard, EldersCard,
  FaeMerchantCard, MessengerCard, SiegeEnginesCard, WolvesCard}
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, Outcomes, PhasePowerHarness}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted
import distantlands.testkit.{AppendedAutomaticUses, Table}

/** Hospitality: after its holder travels, 1 favor from a bank matching both
  * a card at the destination and one of the holder's faceup advisers. */
class HospitalitySuite extends munit.FunSuite:
  import PowerFixture._
  import TravelFixture._

  private val hospitality = HospitalityCard.id
  /** Order. */
  private val messenger = MessengerCard.id
  /** Beast. */
  private val wolves = WolvesCard.id
  /** Nomad. */
  private val elders = EldersCard.id
  /** Order. */
  private val siegeEngines = SiegeEnginesCard.id
  /** Beast. */
  private val faeMerchant = FaeMerchantCard.id
  /** Arcane. */
  private val bloodPact = BloodPactCard.id

  /** The actor at Buried Giant holds Hospitality (Nomad), Messenger (Order)
    * and Wolves (Beast) faceup, or Hospitality facedown when
    * `hospitalityFacedown`. `atCoast` stand at the coast, and only `stocks`
    * hold favor. */
  private def holdingHospitalityAndCoastHolding(atCoast: Vector[DenizenId],
      stocks: (Suit, Int)*)(hospitalityFacedown: Boolean = false)
      : ReadyGame =
    on(board(supply = 7))(table => atCoast.foldLeft(table
      .adviser(actor, hospitality, facedown = hospitalityFacedown)
      .adviser(actor, messenger).adviser(actor, wolves)
      .banksHoldingOnly(stocks*))((placed, card) =>
        placed.denizen(card, coast)))

  private def bank(suit: Suit): DecisionOptionRef.FavorBank =
    DecisionOptionRef.FavorBank(suit)

  private def favorGains(events: Vector[OathEvent]): Vector[Gain.Favor] =
    PhasePowerHarness.ops(events).collect { case gain: Gain.Favor => gain }

  test("with two stocked matching banks it asks after the move, shows an " +
      "empty one unavailable, and the chosen bank gives 1 favor"):
    val ready = holdingHospitalityAndCoastHolding(
      Vector(elders, siegeEngines, faeMerchant),
      Suit.Nomad -> 2, Suit.Order -> 2)()
    val asked = accepted(travel(ready, coast))
    val decide = parkedDecision(asked.state)
    assertEquals(decide.decisionId, Hospitality.decisionId)
    assertEquals(decide.query, DecisionQuery.ChooseOne(
      Suit.all.filter(Set(Suit.Order, Suit.Beast, Suit.Nomad)).map(suit =>
        val shown = DecisionOption.FavorBank(bank(suit))
        if suit == Suit.Beast then DecisionOption.Unavailable(shown,
          OptionReason.Written(Hospitality.emptyBank))
        else shown),
      heading = Some(Hospitality.heading)))
    // The pawn arrived before the question.
    assertEquals(Look(Outcomes.ready(asked.state)).pawn(actor), coast)
    val done = accepted(answeringStep(asked, Hospitality.decisionId,
      bank(Suit.Order)))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).favor(actor), Look(ready).favor(actor) + 1)
    assertEquals(Look(result).bankFavor(Suit.Order), 1)
    assertEquals(Look(result).bankFavor(Suit.Nomad), 2)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), result)
    assertWireRoundTrips(done.events)

  test("an empty bank cannot be chosen"):
    val ready = holdingHospitalityAndCoastHolding(
      Vector(elders, siegeEngines, faeMerchant),
      Suit.Nomad -> 2, Suit.Order -> 2)()
    val asked = accepted(travel(ready, coast))
    assert(answeringStep(asked, Hospitality.decisionId, bank(Suit.Beast))
      .isLeft)

  test("one stocked matching bank gives its favor without asking"):
    val ready = holdingHospitalityAndCoastHolding(Vector(elders, siegeEngines),
      Suit.Nomad -> 2)()
    val done = accepted(travel(ready, coast))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).pawn(actor), coast)
    assertEquals(favorGains(done.events),
      Vector(Gain.Favor(actor, Suit.Nomad, 1)))
    assertEquals(Look(result).bankFavor(Suit.Nomad), 1)

  test("a bank matching only a card at the site, or only an adviser, gives " +
      "nothing"):
    // Arcane is at the coast but not among the advisers; Order is among the
    // advisers but not at the coast.
    val ready = holdingHospitalityAndCoastHolding(Vector(bloodPact),
      Suit.Arcane -> 2, Suit.Order -> 2)()
    val done = accepted(travel(ready, coast))
    assertEquals(favorGains(done.events), Vector.empty)

  test("matching banks that hold no favor give nothing"):
    val ready = holdingHospitalityAndCoastHolding(Vector(elders))()
    val done = accepted(travel(ready, coast))
    assertEquals(favorGains(done.events), Vector.empty)

  test("an edifice at the site matches as a denizen does"):
    // Great Forge is a Nomad edifice; Hospitality itself is the Nomad adviser.
    val greatForge = GreatForgeCard.id
    val ready = on(holdingHospitalityAndCoastHolding(Vector.empty,
      Suit.Nomad -> 2)())(_.edifice(greatForge, EdificeSide.Intact, coast))
    val done = accepted(travel(ready, coast))
    assertEquals(favorGains(done.events),
      Vector(Gain.Favor(actor, Suit.Nomad, 1)))

  test("a facedown Hospitality does nothing"):
    // Siege Engines matches the faceup Messenger, so only the facedown
    // Hospitality stops the gain.
    val ready = holdingHospitalityAndCoastHolding(Vector(siegeEngines),
      Suit.Order -> 2)(hospitalityFacedown = true)
    val done = accepted(travel(ready, coast))
    assertEquals(favorGains(done.events), Vector.empty)

  /** p2's pawn moving from Ancient City to Deep Woods in a travel's cost
    * node, folded with p1 acting. */
  private val p2Travels: Operation = Sequence(Vector[Operation](Move(
    Piece.Pawn(Table.p2), PositionedLocation(Location.Site(
      AncientCitySite.id)), PositionedLocation(Location.Site(
      DeepWoodsSite.id)))), Some(PowerWindow.TravelCost))

  test("its gain is an automatic use of the traveller's, judged where it " +
      "runs"):
    assertEquals(AppendedAutomaticUses.at(Hospitality,
      PowerWindow.TravelCost, p2Travels), Vector(AppendedAutomaticUses.Named(
      Table.p2, Hospitality.id, HospitalityCard.id)))
