package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import distantlands.gameplay.chronicle.{BuildEdifice, ChronicleWalk}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Great Architect (L07): "If you are an Imperial: you perform Build
  * Edifice and may build any edifice (not just a matching one)." (rulings 10
  * and 25: the holder buries one card, then picks the available edifice). p1, the Chancellor, holds the Throne; the Empire rules Ancient
  * City, which holds Threatening Roar. */
class GreatArchitectSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val roar = ThreateningRoarCard.id
  private val roarSuit = catalog.denizen(roar).get.suit
  /** A suit other than the denizen's, whose edifice it may build. */
  private val other = Suit.all.find(_ != roarSuit).get

  private def empire: Table = ChronicleWalk.throneHeldByChancellorP1
    .imperial(p1, p2).warbandsAt(AncientCitySite, p1, 1)
    .denizen(ThreateningRoarCard, at = AncientCitySite)

  private def throne(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def buryingRoar: PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == BuildEdifice.cardDecisionId =>
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Denizen(roar))

  /** Buries the roar and picks the available edifice of `suit`. */
  private def building(suit: Suit): PartialFunction[Decide, DecisionAnswer] =
    buryingRoar.orElse:
      case decide if decide.decisionId == BuildEdifice.edificeDecisionId =>
        DecisionAnswer.ChooseOneAnswer(decide.query.asInstanceOf[
          DecisionQuery.ChooseOne].options.map(_.ref).collectFirst {
            case ref @ DecisionOptionRef.Edifice(id)
                if catalog.edifice(id).exists(_.suit == suit) => ref }.get)

  private def builds(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId.startsWith("chronicle.build-edifice."))

  private def edificesAt(ready: ReadyGame): Vector[EdificeId] =
    ready.game.current.map.sites(AncientCitySite.id).denizens.collect {
      case EdificeState(id, _, _) => id }

  test("a Citizen holder performs Build Edifice in place of the Chancellor " +
      "and builds an off-suit edifice (ruling 10)"):
    val run = throne(empire.legacy(p2, GreatArchitectCard), building(other))
    assert(builds(run).nonEmpty && builds(run).forall(_.owner == p2))
    assertEquals(edificesAt(run.ready).flatMap(id =>
      catalog.edifice(id).map(_.suit)), Vector(other))
    assert(!run.ready.game.current.map.sites(AncientCitySite.id).denizens
      .exists(_.id == roar))

  test("the card is chosen first, then any available edifice, whatever " +
      "the card's suit"):
    val run = throne(empire.legacy(p2, GreatArchitectCard), buryingRoar)
    assertEquals(builds(run).map(_.decisionId),
      Vector(BuildEdifice.cardDecisionId, BuildEdifice.edificeDecisionId))
    assertEquals(builds(run).head.query.asInstanceOf[DecisionQuery.ChooseOne]
      .options.map(_.ref), Vector[DecisionOptionRef](
        DecisionOptionRef.Denizen(roar), BuildEdifice.skip))
    // The fixture's edifice deck holds every suit, so each suit offers one.
    val edifices = builds(run)(1).query.asInstanceOf[DecisionQuery.ChooseOne]
      .options.collect { case DecisionOption.Edifice(DecisionOptionRef.Edifice(
        id)) => catalog.edifice(id).get.suit }
    assertEquals(edifices, Suit.all)

  test("building nothing asks for no edifice"):
    val run = throne(empire.legacy(p2, GreatArchitectCard))
    assertEquals(builds(run).map(_.decisionId),
      Vector(BuildEdifice.cardDecisionId))
    assertEquals(edificesAt(run.ready), Vector.empty)

  test("a lone available edifice is placed without asking which"):
    val table = empire.legacy(p2, GreatArchitectCard)
    val deck = table.ready.game.current.commonCards.edificeDeck
    val lone = deck.find(id => catalog.edifice(id).exists(_.suit == other)).get
    // Every other edifice stands intact at Buried Giant, which the Empire
    // does not rule, so only one is available to build.
    val elsewhere = deck.filterNot(_ == lone)
    val run = throne(table.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(edificeDeck = Vector(lone)),
      map = current.map.copy(sites = current.map.sites.updatedWith(
        BuriedGiantSite.id)(_.map(site => site.copy(denizens = site.denizens ++
          elsewhere.map(EdificeState(_, EdificeSide.Intact,
            Tokens.empty))))))))), buryingRoar)
    assertEquals(builds(run).map(_.decisionId),
      Vector(BuildEdifice.cardDecisionId))
    assertEquals(edificesAt(run.ready), Vector(lone))

  test("an off-suit burial returns the buried card's favor to its own " +
      "suit's bank (R14)"):
    val table = empire.tokens(ThreateningRoarCard, favor = 2)
      .bankFavor(roarSuit, 0).bankFavor(other, 0)
      .legacy(p2, GreatArchitectCard)
    val run = throne(table, building(other))
    assertEquals((run.ready.banks.favor(roarSuit),
      run.ready.banks.favor(other)), (2, 0))

  test("held by the Chancellor, only the matching rule changes"):
    val run = throne(empire.legacy(p1, GreatArchitectCard), building(other))
    assert(builds(run).forall(_.owner == p1))
    assertEquals(edificesAt(run.ready).flatMap(id =>
      catalog.edifice(id).map(_.suit)), Vector(other))

  test("an Exile's Great Architect does nothing (ruling 2)"):
    val run = throne(empire.legacy(p3, GreatArchitectCard))
    assert(builds(run).forall(_.owner == p1))
    assertEquals(builds(run).map(_.decisionId),
      Vector(BuildEdifice.cardDecisionId))
    assertEquals(builds(run).head.query.asInstanceOf[DecisionQuery.ChooseOne]
      .options.map(_.ref), Vector[DecisionOptionRef](
        DecisionOptionRef.Denizen(roar), BuildEdifice.skip))
