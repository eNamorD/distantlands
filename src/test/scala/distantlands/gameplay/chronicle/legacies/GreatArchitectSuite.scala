package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import distantlands.gameplay.chronicle.{BuildEdifice, ChronicleWalk}
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.WildPasturesCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Great Architect (L07): "If you are an Imperial: you perform Build
  * Edifice and may build any edifice (not just a matching one)." (ruling
  * 10). p1, the Chancellor, holds the Throne; the Empire rules Ancient
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

  private def building(suit: Suit): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == BuildEdifice.decisionId(suit) =>
      DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Denizen(roar)))

  private def builds(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId.startsWith("chronicle.build-edifice."))

  private def edificesAt(ready: ReadyGame): Vector[EdificeId] =
    ready.game.current.map.sites(AncientCitySite.id).denizens.collect {
      case EdificeState(id, _, _) => id }

  test("Great Architect is implemented"):
    assert(PowerImplementationStatus.implemented(GreatArchitectCard.power.id))

  test("a Citizen holder performs Build Edifice in place of the Chancellor " +
      "and builds an off-suit edifice (ruling 10)"):
    val run = throne(empire.legacy(p2, GreatArchitectCard), building(other))
    assert(builds(run).nonEmpty && builds(run).forall(_.owner == p2))
    assertEquals(edificesAt(run.ready).flatMap(id =>
      catalog.edifice(id).map(_.suit)), Vector(other))
    assert(!run.ready.game.current.map.sites(AncientCitySite.id).denizens
      .exists(_.id == roar))

  test("every available edifice is offered the denizen, whatever its suit"):
    val run = throne(empire.legacy(p2, GreatArchitectCard))
    // The fixture's edifice deck holds every suit, so each suit offers one.
    assertEquals(builds(run).map(_.decisionId),
      Suit.all.map(BuildEdifice.decisionId))
    assert(builds(run).forall(_.query.asInstanceOf[DecisionQuery.ChooseMany]
      .options.map(_.ref) == Vector(DecisionOptionRef.Denizen(roar))))

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
      Vector(BuildEdifice.decisionId(roarSuit)))

  test("a walk resumed at a later suit's question after an off-suit build " +
      "finds the denizen gone and the new edifice offered"):
    // The first suit's build buries the only denizen; each later suit,
    // asked of a walk rebuilt from the journal, offers the edifice just
    // built in its place, as an edifice may be buried.
    val first = Suit.all.head
    val run = throne(empire.legacy(p2, GreatArchitectCard), building(first))
    val built = edificesAt(run.ready)
    assertEquals(built.size, 1)
    val later = builds(run).drop(1)
    assertEquals(later.map(_.decisionId),
      Suit.all.drop(1).map(BuildEdifice.decisionId))
    assert(later.forall(_.query.asInstanceOf[DecisionQuery.ChooseMany]
      .options.map(_.ref) == Vector(DecisionOptionRef.Edifice(built.head))))

  /** The Empire rules Ancient City, which holds a ruined Wild Pastures, the
    * only Nomad edifice left to build: every other stands intact at Buried
    * Giant, which the Empire does not rule. p2 holds Great Architect. */
  private def lonePastures: Table =
    val base = ChronicleWalk.throneHeldByChancellorP1.imperial(p1, p2)
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(WildPasturesCard, EdificeSide.Ruined, at = AncientCitySite)
      .legacy(p2, GreatArchitectCard)
    val nomads = base.ready.game.current.commonCards.edificeDeck
      .filter(id => catalog.edifice(id).exists(_.suit == Suit.Nomad))
    base.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(edificeDeck =
        current.commonCards.edificeDeck.filterNot(nomads.contains)),
      map = current.map.copy(sites = current.map.sites.updatedWith(
        BuriedGiantSite.id)(_.map(site => site.copy(denizens = site.denizens ++
          nomads.map(EdificeState(_, EdificeSide.Intact, Tokens.empty)))))))))

  private val buryPastures: PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == BuildEdifice.decisionId(Suit.Discord) =>
      DecisionAnswer.ChooseManyAnswer(Vector(
        DecisionOptionRef.Edifice(WildPasturesCard.id)))

  test("an edifice buried earlier in the step is not offered by a later " +
      "suit (R45)"):
    val run = throne(lonePastures, buryPastures)
    assert(edificesAt(run.ready).forall(_ != WildPasturesCard.id))
    assert(!builds(run).exists(_.decisionId ==
      BuildEdifice.decisionId(Suit.Nomad)), builds(run).map(_.decisionId))

  test("a walk resumed at a later suit's question still leaves out the " +
      "edifice buried earlier (R45)"):
    val table = lonePastures
    val atDiscord = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleSun),
      BuildEdifice.decisionId(Suit.Discord))
    val buried = accepted(rules.resolveWalker(atDiscord.state, p2,
      BuildEdifice.decisionId(Suit.Discord), DecisionAnswer.ChooseManyAnswer(
        Vector(DecisionOptionRef.Edifice(WildPasturesCard.id)))))
    val events = atDiscord.events ++ buried.events
    val replayed = accepted(events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    val parked = ChronicleWalk.parked(replayed)
    assertEquals(parked, ChronicleWalk.parked(buried.state))
    assertEquals(parked.map(_.decisionId),
      Some(BuildEdifice.decisionId(Suit.Arcane)))
    // Each later suit, answered plainly from the replayed walk, asks no
    // Nomad question.
    val asked = Iterator.iterate(Option(replayed))(_.flatMap(state =>
      ChronicleWalk.parked(state).filter(_.decisionId.startsWith(
        "chronicle.build-edifice.")).map(decide => accepted(
        rules.resolveWalker(state, decide.owner, decide.decisionId,
          ChronicleWalk.plainestAnswer(decide))).state)))
      .takeWhile(_.nonEmpty).flatten.flatMap(ChronicleWalk.parked).toVector
      .map(_.decisionId).filter(_.startsWith("chronicle.build-edifice."))
    assert(!asked.contains(BuildEdifice.decisionId(Suit.Nomad)), asked)
