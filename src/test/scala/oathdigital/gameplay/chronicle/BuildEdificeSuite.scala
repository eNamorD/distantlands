package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Outcomes, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Build Edifice (CR p. 37, Chronicle rulings 9 and 12). p1 is the
  * Chancellor holding the Throne, and the Empire rules Ancient City. The
  * shuffle reverses the edifice deck, so a suit's offer is the last edifice
  * of that suit in the deck the table started with. */
class BuildEdificeSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = CatalogNames.site("Ancient City")
  private val roar = CatalogNames.denizen("Threatening Roar")
  private val roarSuit = catalog.denizen(roar).get.suit

  private val empireAtAncientCity: Table =
    ChronicleWalk.throneHeldByChancellorP1.warbandsAt("Ancient City", p1, 1)

  private def suitOf(id: EdificeId): Option[Suit] = catalog.edifice(id).map(_.suit)

  /** The edifice `suit` offers on `table`, after the reversing shuffle. */
  private def offered(table: Table, suit: Suit)(using munit.Location)
      : EdificeId =
    val current = table.ready.game.current
    (current.commonCards.edificeDeck.reverse ++ current.setAsideEdifices)
      .find(id => suitOf(id).contains(suit)).get

  /** The Throne started and walked to the Build Edifice choice for `suit`,
    * every earlier choice answered plainly. */
  private def atBuildChoice(table: Table, suit: Suit)(using munit.Location)
      : OathTransition =
    val started = accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))
    Iterator.iterate(started)(step =>
      val decide = ChronicleWalk.parked(step.state).get
      val next = accepted(rules.resolveWalker(step.state, decide.owner,
        decide.decisionId, ChronicleWalk.plainestAnswer(decide)))
      OathTransition(next.state, step.events ++ next.events))
      .find(step => ChronicleWalk.parked(step.state)
        .forall(_.decisionId == BuildEdifice.decisionId(suit))).get

  private def build(transition: OathTransition, suit: Suit,
      picked: Vector[DecisionOptionRef])(using munit.Location)
      : OathTransition =
    val next = accepted(rules.resolveWalker(transition.state, p1,
      BuildEdifice.decisionId(suit),
      DecisionAnswer.ChooseManyAnswer(picked)))
    ChronicleWalk.answeringEverything(rules,
      OathTransition(next.state, transition.events ++ next.events))

  private def denizensAt(transition: OathTransition, site: SiteId)
      : Vector[SiteDenizenState] =
    Outcomes.ready(transition.state).game.current.map.sites(site).denizens

  test("a suit with a buriable card offers its edifice to the Throne holder"):
    val at = atBuildChoice(empireAtAncientCity
      .denizen("Threatening Roar", at = "Ancient City"), roarSuit)
    val parked = ChronicleWalk.parked(at.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max, many.options.map(_.ref)),
          (0, 1, Vector[DecisionOptionRef](DecisionOptionRef.Denizen(roar))))
      case other => fail(other.toString)

  test("burying a denizen builds the offered edifice intact"):
    val table = empireAtAncientCity
      .denizen("Threatening Roar", at = "Ancient City")
    val edifice = offered(table, roarSuit)
    val done = build(atBuildChoice(table, roarSuit), roarSuit,
      Vector(DecisionOptionRef.Denizen(roar)))
    assertEquals(denizensAt(done, ancientCity),
      Vector(EdificeState(edifice, EdificeSide.Intact, Tokens.empty)))
    assertEquals(Outcomes.ready(done.state).game.current.commonCards
      .worldDeck.last, roar)

  test("an intact edifice can be buried for another of its suit"):
    val old = CatalogNames.edifice("Great Market")
    val suit = suitOf(old).get
    val table = empireAtAncientCity
      .edifice("Great Market", EdificeSide.Intact, at = "Ancient City")
    val edifice = offered(table, suit)
    val done = build(atBuildChoice(table, suit), suit,
      Vector(DecisionOptionRef.Edifice(old)))
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(edifice))
    assert(Outcomes.ready(done.state).game.current.commonCards.edificeDeck
      .contains(old))

  test("declining builds nothing"):
    val done = build(atBuildChoice(empireAtAncientCity
      .denizen("Threatening Roar", at = "Ancient City"), roarSuit), roarSuit,
      Vector.empty)
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(roar))

  test("a suit with no edifice left in the deck offers its first set-aside " +
      "one, and the set-aside list returns to the deck after"):
    val start = empireAtAncientCity
      .denizen("Threatening Roar", at = "Ancient City")
    val ofSuit = start.ready.game.current.commonCards.edificeDeck
      .filter(id => suitOf(id).contains(roarSuit))
    val table = start.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(edificeDeck =
        current.commonCards.edificeDeck.filterNot(ofSuit.contains)),
      setAsideEdifices = ofSuit)))
    val done = build(atBuildChoice(table, roarSuit), roarSuit,
      Vector(DecisionOptionRef.Denizen(roar)))
    val after = Outcomes.ready(done.state).game.current
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(ofSuit.head))
    assertEquals(after.setAsideEdifices, Vector.empty)
    assert(ofSuit.tail.forall(after.commonCards.edificeDeck.contains))

  test("cards at sites the Empire does not rule are not offered"):
    val at = atBuildChoice(empireAtAncientCity
      .denizen("Threatening Roar", at = "Broken Peaks"), roarSuit)
    assert(!ChronicleWalk.parked(at.state)
      .exists(_.decisionId == BuildEdifice.decisionId(roarSuit)))

  test("building writes its line"):
    val table = empireAtAncientCity
      .denizen("Threatening Roar", at = "Ancient City")
    val edifice = offered(table, roarSuit)
    val done = build(atBuildChoice(table, roarSuit), roarSuit,
      Vector(DecisionOptionRef.Denizen(roar)))
    val id = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
    assert(NoteText.said(id, ChronicleThroneProcedure.noteKeys, done.events)
      .map(_.text).contains(s"${p1.value} built ${edifice.value} at " +
        s"${ancientCity.value}"), done.events)
