package distantlands.gameplay.chronicle

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.application.{GameProjector, LoadedGame}
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Build Edifice (CR p. 37, Chronicle rulings 9, 12 and 25). p1 is the
  * Chancellor holding the Throne, and the Empire rules Ancient City. The
  * shuffle reverses the edifice deck, so a suit's available edifice is the
  * last edifice of that suit in the deck the table started with. */
class BuildEdificeSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = AncientCitySite.id
  private val roar = ThreateningRoarCard.id
  private val roarSuit = catalog.denizen(roar).get.suit

  private val empireAtAncientCity: Table =
    ChronicleWalk.throneHeldByChancellorP1.warbandsAt(AncientCitySite, p1, 1)

  private def suitOf(id: EdificeId): Option[Suit] = catalog.edifice(id).map(_.suit)

  /** The edifice `suit` offers on `table`, after the reversing shuffle. */
  private def offered(table: Table, suit: Suit)(using munit.Location)
      : EdificeId =
    val current = table.ready.game.current
    (current.commonCards.edificeDeck.reverse ++ current.setAsideEdifices)
      .find(id => suitOf(id).contains(suit)).get

  /** The Throne started and walked to the Build Edifice choice, every
    * earlier choice answered plainly. */
  private def atBuildChoice(table: Table)(using munit.Location)
      : OathTransition =
    val started = accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))
    ChronicleWalk.walkedTo(rules, started, BuildEdifice.cardDecisionId)

  private def build(transition: OathTransition, picked: DecisionOptionRef)(
      using munit.Location): OathTransition =
    val next = accepted(rules.resolveWalker(transition.state, p1,
      BuildEdifice.cardDecisionId, DecisionAnswer.ChooseOneAnswer(picked)))
    ChronicleWalk.answeringEverything(rules,
      OathTransition(next.state, transition.events ++ next.events))

  private def denizensAt(transition: OathTransition, site: SiteId)
      : Vector[SiteDenizenState] =
    Outcomes.ready(transition.state).game.current.map.sites(site).denizens

  test("the Throne holder may bury any card they rule, or build nothing"):
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val at = atBuildChoice(table)
    val parked = ChronicleWalk.parked(at.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector[DecisionOptionRef](
          DecisionOptionRef.Denizen(roar), BuildEdifice.skip))
        assertEquals(one.heading,
          Some("Bury a card to build the available matching edifice"))
      case other => fail(other.toString)

  test("every viewer, a spectator too, sees the available edifices as " +
      "faceup cards beside the question"):
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val at = atBuildChoice(table)
    val available = Suit.all.map(offered(table, _).value)
    val projector = new GameProjector(catalog)
    val loaded = LoadedGame(at.state, 1L)
    Vector(projector.project("game", loaded, p1),
      projector.project("game", loaded, PlayerId("p2")),
      projector.projectPublic("game", loaded)).foreach: seen =>
      val preview = seen.publicPreview.get.sections.head
      assertEquals(seen.publicPreview.get.sections.size, 1)
      assertEquals(preview.title, "Available edifices")
      assertEquals(preview.cards.map(_.cardId), available)
      assert(preview.cards.forall(card => card.cardKind == "edifice" &&
        !card.hidden), preview.cards.toString)

  test("no other question carries a public preview"):
    val at = atBuildChoice(empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite))
    val done = build(at, BuildEdifice.skip)
    assertEquals(new GameProjector(catalog).project("game",
      LoadedGame(done.state, 1L), p1).publicPreview, None)

  test("a card of every suit is offered, the available edifice of its " +
      "suit is built, and no other"):
    val other = empireAtAncientCity.ready.game.current.commonCards.worldDeck
      .collectFirst { case id: DenizenId
        if catalog.denizen(id).exists(_.suit != roarSuit) => id }.get
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .denizen(other, at = AncientCitySite)
    val at = atBuildChoice(table)
    assertEquals(ChronicleWalk.parked(at.state).get.query.asInstanceOf[
      DecisionQuery.ChooseOne].options.map(_.ref).toSet,
      Set[DecisionOptionRef](DecisionOptionRef.Denizen(roar),
        DecisionOptionRef.Denizen(other), BuildEdifice.skip))
    val done = build(at, DecisionOptionRef.Denizen(other))
    val otherSuit = catalog.denizen(other).get.suit
    assertEquals(denizensAt(done, ancientCity),
      Vector(DenizenState(roar, Orientation.FaceUp, Tokens.empty),
        EdificeState(offered(table, otherSuit), EdificeSide.Intact,
          Tokens.empty)))

  test("burying a denizen builds the available edifice of its suit intact"):
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val edifice = offered(table, roarSuit)
    val done = build(atBuildChoice(table), DecisionOptionRef.Denizen(roar))
    assertEquals(denizensAt(done, ancientCity),
      Vector(EdificeState(edifice, EdificeSide.Intact, Tokens.empty)))
    // Read before the World deals its surplus under the buried card.
    val throneEnded = ChronicleWalk.endOf(rules, table.state, done,
      TriggeredProcedureRef.ChronicleThrone)
    assertEquals(throneEnded.game.current.commonCards.worldDeck.last, roar)

  test("an intact edifice can be buried for another of its suit"):
    val old = GreatMarketCard.id
    val suit = suitOf(old).get
    val table = empireAtAncientCity
      .edifice(GreatMarketCard, EdificeSide.Intact, at = AncientCitySite)
    val edifice = offered(table, suit)
    val done = build(atBuildChoice(table), DecisionOptionRef.Edifice(old))
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(edifice))
    assert(Outcomes.ready(done.state).game.current.commonCards.edificeDeck
      .contains(old))

  test("declining builds nothing"):
    val done = build(atBuildChoice(empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)),
      BuildEdifice.skip)
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(roar))

  test("a suit with no edifice left in the deck offers its first set-aside " +
      "one, and the set-aside list returns to the deck after"):
    val start = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val ofSuit = start.ready.game.current.commonCards.edificeDeck
      .filter(id => suitOf(id).contains(roarSuit))
    val table = start.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(edificeDeck =
        current.commonCards.edificeDeck.filterNot(ofSuit.contains)),
      setAsideEdifices = ofSuit)))
    val done = build(atBuildChoice(table), DecisionOptionRef.Denizen(roar))
    val after = Outcomes.ready(done.state).game.current
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(ofSuit.head))
    assertEquals(after.setAsideEdifices, Vector.empty)
    assert(ofSuit.tail.forall(after.commonCards.edificeDeck.contains))

  test("a card whose suit has no edifice to build is not offered"):
    val start = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    // Every edifice of the suit stands intact at Buried Giant, which the
    // Empire does not rule, so none is left to build.
    val ofSuit = start.ready.game.current.commonCards.edificeDeck
      .filter(id => suitOf(id).contains(roarSuit))
    val table = start.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(edificeDeck =
        current.commonCards.edificeDeck.filterNot(ofSuit.contains)),
      map = current.map.copy(sites = current.map.sites.updatedWith(
        BuriedGiantSite.id)(_.map(site => site.copy(denizens = site.denizens ++
          ofSuit.map(EdificeState(_, EdificeSide.Intact, Tokens.empty)))))))))
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleThrone, PartialFunction.empty,
      after = TriggeredProcedureRef.ChronicleSun)
    assert(!run.asked.exists(_.decisionId == BuildEdifice.cardDecisionId))
    assert(run.ready.game.current.map.sites(ancientCity)
      .denizens.exists(_.id == roar))

  test("cards at sites the Empire does not rule are not offered"):
    val at = atBuildChoice(empireAtAncientCity
      .denizen(ThreateningRoarCard, at = BrokenPeaksSite))
    assert(!ChronicleWalk.parked(at.state)
      .exists(_.decisionId == BuildEdifice.cardDecisionId))

  test("building writes its line"):
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val edifice = offered(table, roarSuit)
    val done = build(atBuildChoice(table), DecisionOptionRef.Denizen(roar))
    val id = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
    assert(NoteText.said(id, ChronicleThroneProcedure.noteKeys, done.events)
      .map(_.text).contains(s"${p1.value} built ${edifice.value} at " +
        s"${ancientCity.value}"), done.events)
