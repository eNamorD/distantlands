package distantlands.gameplay.setup

import distantlands.application.GameCommand
import distantlands.gameplay.cards.GreatSlumSite
import distantlands.gameplay.walker.LogNoted
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Imperial Maps, the normal Foundation I, at a later game's Setup
  * (Foundations design, slice 4): the Chancellor fills the slots past the
  * Empire's sites from the Recent or the Forgotten end of the Atlas. l1 is
  * the seated Chancellor, l2 a Citizen and l3 an Exile; p1 goes first. The
  * Empire holds Ancient City and Broken Peaks, which fill the Cradle, and
  * the Atlas holds Buried Giant at its Recent end, then Deep Woods,
  * Desolate Shore, Dunes, Fair Isle, Golden Valley, and Great Slum at its
  * Forgotten end: one site more than the six empty slots. */
class ImperialMapsSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"), PlayerId("p3"))
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))

  /** The Chronicle described above, Great Slum storing nothing. */
  private val chronicleWithGreatSlumAtForgottenEnd: Chronicle =
    val record = LaterGameFixture.chronicle(l1, l2)
    record.copy(atlasBox = record.atlasBox :+
      StoredSite(GreatSlumSite.id, Vector.empty))

  private def orders(chronicle: Chronicle, first: PlayerId = p1)
      : SetupOrders = LaterGameFixture.orders(chronicle, first)

  /** Answers Imperial Maps with `end`, and every other park as the
    * situation does by default. */
  private def answering(end: AtlasEnd): Answers =
    case park if park.decisionId == ImperialMaps.decisionId =>
      ChooseOneAnswer(AtlasPlacement.endOption(end))

  /** `chronicle`'s Setup run until it parks at `decisionId`, Imperial Maps
    * answered with `end`. */
  private def setupParkedAt(decisionId: String, end: AtlasEnd,
      chronicle: Chronicle = chronicleWithGreatSlumAtForgottenEnd,
      first: PlayerId = p1): Situation =
    Situation.start(Situation.rules(catalog).withAnswers(answering(end)))
      .parkedAt(decisionId)(GameCommand.Begin(chronicle,
        orders(chronicle, first)))

  /** The game once the map is filled from `end`: parked at the
    * Chancellor's pawn question. */
  private def filledFrom(end: AtlasEnd,
      chronicle: Chronicle = chronicleWithGreatSlumAtForgottenEnd)
      : ReadyGame =
    setupParkedAt(SetupProcedure.pawnDecisionId(p1), end, chronicle).ready

  /** The decision `situation` is parked on. */
  private def parkedDecide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  /** The sites a pawn question offers. */
  private def siteOptions(decide: Decide): Vector[DecisionOptionRef] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options.map(_.ref)
      case other => fail(s"expected a choose-one, got $other")

  /** The suit of every denizen on `ready`'s map, edifices included. */
  private def suitsOnMap(ready: ReadyGame): Vector[Suit] =
    ready.game.current.map.sites.values.flatMap(_.denizens)
      .flatMap(AtlasPlacement.suitOf(catalog, _)).toVector

  private val atlasSites: Vector[SiteId] =
    chronicleWithGreatSlumAtForgottenEnd.atlasBox.map(_.site)
  private val empireSites: Vector[SiteId] =
    chronicleWithGreatSlumAtForgottenEnd.world.map(_.site)

  test("game start leaves the slots past the Empire's sites empty, and " +
      "the Chancellor's first question after the Reliquary pick is " +
      "Recent or Forgotten"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val relic = record.relicDeck.head
    val withReliquary = record.copy(relicDeck = record.relicDeck.tail,
      reliquary = Vector(relic))
    val started = accepted(GameStartRules.evolve(catalog, withReliquary,
      orders(withReliquary)))
    assertEquals(started.game.current.map.hinterward, empireSites)
    assertEquals(started.game.campaign.atlas.sites.map(_.id), atlasSites)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(withReliquary, orders(withReliquary)))
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.reliquaryDecisionId)
    val asked = parkedDecide(begun.parkedAt(ImperialMaps.decisionId)())
    assertEquals(asked.owner, p1)
    assertEquals(asked.query, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(DecisionOptionRef.Button("recent"),
        "Recent Sites (Buried Giant first)"),
      DecisionOption.Button(DecisionOptionRef.Button("forgotten"),
        "Forgotten Sites")),
      heading = Some("Imperial Maps: fill the map from")))

  test("the question names only the Atlas's first Recent site, which is " +
      "public: no other site, in no option, line or answer key"):
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(chronicleWithGreatSlumAtForgottenEnd,
        orders(chronicleWithGreatSlumAtForgottenEnd)))
    val asked = parkedDecide(begun)
    assertEquals(asked.decisionId, ImperialMaps.decisionId)
    val hidden = atlasSites.tail.flatMap(site =>
      Vector(site.value, catalog.site(site).get.name))
    val shown = asked.toString
    hidden.foreach(words => assert(!shown.contains(words), words))
    assert(!begun.events.exists {
      case LogNoted(_, note, _) => note.args.exists {
        case NoteArg.Site(site) => atlasSites.contains(site)
        case _ => false
      }
      case _ => false
    })

  test("Recent Sites fill every empty slot going Hinterward from the " +
      "Atlas's head, and the rest stays in the Atlas"):
    val ready = filledFrom(AtlasEnd.Recent)
    val map = ready.game.current.map
    assertEquals(map.hinterward, empireSites ++ atlasSites.take(6))
    assertEquals(map.nextSlot, None)
    assertEquals(ready.game.campaign.atlas.sites.map(_.id),
      Vector(GreatSlumSite.id))

  test("Forgotten Sites take the Atlas's tail first"):
    val ready = filledFrom(AtlasEnd.Forgotten)
    assertEquals(ready.game.current.map.hinterward,
      empireSites ++ atlasSites.reverse.take(6))
    assertEquals(ready.game.campaign.atlas.sites.map(_.id),
      Vector(atlasSites.head))

  test("a placed site keeps its stored cards, its edifice ruined and its " +
      "denizens faceup, and takes bandits up to its capacity (R27)"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val withEdifice = record.atlasBox.find(_.items.exists(
      _.isInstanceOf[EdificeId])).get
    val edifice = withEdifice.items.collectFirst { case id: EdificeId => id }
      .get
    val denizen = record.worldDeck.head
    val relic = record.relicDeck.head
    val stocked = withEdifice.copy(items = Vector(edifice, denizen, relic))
    val chronicle = record.copy(relicDeck = record.relicDeck.tail,
      worldDeck = record.worldDeck.tail, atlasBox = record.atlasBox.map(
        stored => if stored.site == stocked.site then stocked else stored))
    AtlasEnd.all.foreach { end =>
      val site = filledFrom(end, chronicle).game.current.map
        .sites(stocked.site)
      val capacity = catalog.site(stocked.site).get.capacity
      assertEquals(site.forces,
        SiteForces.Occupied(ForceKind.Bandit, capacity), end)
      assertEquals(site.denizens, Vector(
        EdificeState(edifice, EdificeSide.Ruined, Tokens.empty),
        DenizenState(denizen, Orientation.FaceUp, Tokens.empty)), end)
      assertEquals(site.relics.size,
        math.max(1, catalog.site(stocked.site).get.relicSlots), end)
      assertEquals(site.relics.head,
        RelicState(relic, Orientation.FaceDown, Tokens.empty), end)
    }

  test("each placed site's empty relic slots are dealt facedown from the " +
      "top of the relic deck, in the order the sites are placed"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    AtlasEnd.all.foreach { end =>
      val ready = filledFrom(end)
      val placed = ready.game.current.map.hinterward.drop(2)
      val counts = placed.map(catalog.site(_).get.relicSlots)
      val dealt = counts.scanLeft(0)(_ + _).zip(counts).map((from, count) =>
        record.relicDeck.slice(from, from + count))
      placed.zip(dealt).foreach((site, relics) => assertEquals(
        ready.game.current.map.sites(site).relics, relics.map(
          RelicState(_, Orientation.FaceDown, Tokens.empty)), (end, site)))
      assertEquals(ready.game.current.commonCards.relicDeck,
        record.relicDeck.drop(counts.sum), end)
    }

  test("a relic deck too short for the placed sites leaves the last relic " +
      "slots open, and game start does not refuse it"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val oneRelic = record.copy(relicDeck = record.relicDeck.take(1))
    val ready = filledFrom(AtlasEnd.Recent, oneRelic)
    val placed = ready.game.current.map.hinterward.drop(2)
    val first = placed.find(catalog.site(_).get.relicSlots > 0).get
    assertEquals(placed.flatMap(ready.game.current.map.sites(_).relics),
      Vector(RelicState(oneRelic.relicDeck.head, Orientation.FaceDown,
        Tokens.empty)))
    assertEquals(ready.game.current.map.sites(first).relics.map(_.id),
      oneRelic.relicDeck)
    assertEquals(ready.game.current.commonCards.relicDeck, Vector.empty)

  test("each suit's bank ends with 3 favor and 1 per denizen of its suit " +
      "on the map, the placed sites' included, from either end"):
    AtlasEnd.all.foreach { end =>
      val ready = filledFrom(end)
      val onMap = suitsOnMap(ready)
      assert(onMap.size > 2, "the placed sites' edifices are on the map")
      assertEquals(ready.banks.favor, Suit.all.map(suit =>
        suit -> (3 + onMap.count(_ == suit))).toMap, end)
    }

  test("under Movers and Shakers each bank holds only the 1 favor per " +
      "denizen of its suit on the map"):
    val movers = chronicleWithGreatSlumAtForgottenEnd.copy(foundations = Map(
      FoundationNumber.II -> FoundationState(FoundationFace.Altered,
        Set.empty)))
    val ready = filledFrom(AtlasEnd.Forgotten, movers)
    val onMap = suitsOnMap(ready)
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)

  test("with five players Powerful Tribes leaves each bank 4 favor and 1 " +
      "per denizen of its suit on the map once the map is filled"):
    val record = chronicleWithGreatSlumAtForgottenEnd
    val fiveSeats = LaterGameFixture.seats(5)
    AtlasEnd.all.foreach { end =>
      val ready = Situation.start(Situation.rules(catalog)
          .withAnswers(answering(end)))
        .parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
          record, LaterGameFixture.orders(record, p1, fiveSeats))).ready
      assertEquals(ready.game.current.map.nextSlot, None, end)
      val onMap = suitsOnMap(ready)
      assertEquals(ready.banks.favor, Suit.all.map(suit =>
        suit -> (4 + onMap.count(_ == suit))).toMap, end)
    }

  test("the filled game holds every card once"):
    AtlasEnd.all.foreach(end =>
      assert(filledFrom(end).game.cardIndex.isRight, end))

  test("an Exile may place their pawn at the Hintermost Empire site or at " +
      "any placed site the bandits rule"):
    AtlasEnd.all.foreach { end =>
      val parked = setupParkedAt(SetupProcedure.pawnDecisionId(p3), end)
      val map = parked.ready.game.current.map
      val placed = map.hinterward.drop(2).filter(site =>
        catalog.site(site).get.capacity > 0)
      assertEquals(siteOptions(parkedDecide(parked)),
        (empireSites.last +: placed).map(DecisionOptionRef.Site(_)), end)
    }

  test("the log says which end filled the map, then names each site"):
    val parked = setupParkedAt(SetupProcedure.pawnDecisionId(p1),
      AtlasEnd.Forgotten)
    val lines = parked.events.collect {
      case LogNoted(_, note, _) if ImperialMaps.noteKeys.exists(
        _.name == note.key) => note.key -> note.args
    }
    assertEquals(lines, ("filled-from-forgotten",
        Vector[NoteArg](NoteArg.Player(p1))) +:
      atlasSites.reverse.take(6).map(site => ("placed-site",
        Vector[NoteArg](NoteArg.Player(p1), NoteArg.Site(site)))))

  test("a Citizen standing in for an absent Chancellor answers Imperial " +
      "Maps"):
    val record = LaterGameFixture.chronicle(LineageId("l9"), l2)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(record, orders(record, p2)))
    assertEquals(parkedDecide(begun).decisionId, ImperialMaps.decisionId)
    assertEquals(parkedDecide(begun).owner, p2)

  test("with one site in the Atlas nothing is asked: it fills the last " +
      "slot, and the first question is the keep"):
    val record = LaterGameFixture.chronicle(l1, Vector(l2), 7)
    assertEquals(record.atlasBox.size, 1)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(record, orders(record)))
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.legacyDecisionId(p1))
    assertEquals(begun.ready.game.current.map.hinterward,
      (record.world ++ record.atlasBox).map(_.site))
    assertEquals(begun.events.collect {
      case LogNoted(_, note, _) if ImperialMaps.noteKeys.exists(
        _.name == note.key) => note.key
    }, Vector("filled-from-recent", "placed-site"))

  test("an Empire of one site or of three has every slot past it filled " +
      "going Hinterward, from either end"):
    Vector(1, 3).foreach { size =>
      val record = LaterGameFixture.chronicle(l1, Vector(l2), size)
      val atlas = record.atlasBox.map(_.site)
      assertEquals(record.world.size + atlas.size, MapState.slots, size)
      Vector(AtlasEnd.Recent -> atlas, AtlasEnd.Forgotten -> atlas.reverse)
        .foreach { (end, placed) =>
          val map = filledFrom(end, record).game.current.map
          assertEquals(map.hinterward, record.world.map(_.site) ++ placed,
            (size, end))
          assertEquals(Region.all.map(map.sitesIn(_).size), Vector(2, 3, 3),
            (size, end))
        }
    }

  test("an Empire of eight sites leaves no slot: nothing is asked or " +
      "placed"):
    val record = LaterGameFixture.chronicle(l1, Vector(l2), 8)
    val begun = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(record, orders(record)))
    assertEquals(parkedDecide(begun).decisionId,
      SetupProcedure.legacyDecisionId(p1))
    assert(!begun.events.exists {
      case LogNoted(_, note, _) => ImperialMaps.noteKeys.exists(
        _.name == note.key)
      case _ => false
    })

  test("Wide Horizons with an Empire, Setup With No Empire and a first " +
      "game ask no Imperial Maps question and fill no slot: the first " +
      "question is the keep in a later game and the pawn in a first"):
    val wideHorizons = chronicleWithGreatSlumAtForgottenEnd.copy(
      foundations = Map(FoundationNumber.I -> FoundationState(
        FoundationFace.Altered, Set.empty)))
    val noEmpire = chronicleWithGreatSlumAtForgottenEnd.copy(
      lineages = Vector.empty)
    val firstGame = FirstGameSetupFixture.chronicle
    Vector(
      (wideHorizons, orders(wideHorizons), empireSites,
        SetupProcedure.legacyDecisionId(p1)),
      (noEmpire, orders(noEmpire), empireSites,
        SetupProcedure.legacyDecisionId(p1)),
      (firstGame, FirstGameSetupFixture.orders,
        firstGame.atlasBox.map(_.site),
        SetupProcedure.pawnDecisionId(FirstGameSetupFixture.firstPlayer)))
      .foreach {
      (chronicle, setupOrders, sites, first) =>
        val begun = Situation.start(Situation.rules(catalog))
          .parkedAfter(GameCommand.Begin(chronicle, setupOrders))
        assertEquals(parkedDecide(begun).decisionId, first, chronicle.name)
        assertEquals(begun.ready.game.current.map.hinterward, sites,
          chronicle.name)
    }

  test("game start still refuses an Atlas too short to fill the empty " +
      "slots"):
    val record = LaterGameFixture.chronicle(l1, l2)
    val short = record.copy(atlasBox = record.atlasBox.take(5))
    assertEquals(GameStartRules.evolve(catalog, short, orders(short)),
      Left(OathViolation.UnsupportedChronicle(
        "at least 6 atlas sites are required, got 5")))
