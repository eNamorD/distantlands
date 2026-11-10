package distantlands.gameplay.chronicle

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Build Edifice (CR p. 37, Chronicle rulings 9 and 12). p1 is the
  * Chancellor holding the Throne, and the Empire rules Ancient City. The
  * shuffle reverses the edifice deck, so a suit's offer is the last edifice
  * of that suit in the deck the table started with. */
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

  /** The Throne started and walked to the Build Edifice choice for `suit`,
    * every earlier choice answered plainly. */
  private def atBuildChoice(table: Table, suit: Suit)(using munit.Location)
      : OathTransition =
    val started = accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))
    ChronicleWalk.walkedTo(rules, started,
      BuildEdifice.decisionId(suit))

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
      .denizen(ThreateningRoarCard, at = AncientCitySite), roarSuit)
    val parked = ChronicleWalk.parked(at.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max, many.options.map(_.ref)),
          (0, 1, Vector[DecisionOptionRef](DecisionOptionRef.Denizen(roar))))
      case other => fail(other.toString)

  test("burying a denizen builds the offered edifice intact"):
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val edifice = offered(table, roarSuit)
    val done = build(atBuildChoice(table, roarSuit), roarSuit,
      Vector(DecisionOptionRef.Denizen(roar)))
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
    val done = build(atBuildChoice(table, suit), suit,
      Vector(DecisionOptionRef.Edifice(old)))
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(edifice))
    assert(Outcomes.ready(done.state).game.current.commonCards.edificeDeck
      .contains(old))

  test("declining builds nothing"):
    val done = build(atBuildChoice(empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite), roarSuit), roarSuit,
      Vector.empty)
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
    val done = build(atBuildChoice(table, roarSuit), roarSuit,
      Vector(DecisionOptionRef.Denizen(roar)))
    val after = Outcomes.ready(done.state).game.current
    assertEquals(denizensAt(done, ancientCity).map(_.id), Vector(ofSuit.head))
    assertEquals(after.setAsideEdifices, Vector.empty)
    assert(ofSuit.tail.forall(after.commonCards.edificeDeck.contains))

  test("after building a suit's only edifice, the next suit's answer still " +
      "reaches its own choice"):
    val other = empireAtAncientCity.ready.game.current.commonCards.worldDeck
      .collectFirst { case id: DenizenId
        if catalog.denizen(id).exists(_.suit != roarSuit) => id }.get
    val otherSuit = catalog.denizen(other).get.suit
    val (first, second) =
      if Suit.all.indexOf(roarSuit) < Suit.all.indexOf(otherSuit) then
        (roarSuit -> roar, otherSuit -> other)
      else (otherSuit -> other, roarSuit -> roar)
    val start = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .denizen(other, at = AncientCitySite)
    // Every edifice of the first suit but one stands intact at Buried Giant,
    // which the Empire does not rule, so building leaves that suit none.
    val ofSuit = start.ready.game.current.commonCards.edificeDeck
      .filter(id => suitOf(id).contains(first._1))
    val buriedGiant = BuriedGiantSite.id
    val table = start.update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(edificeDeck =
        current.commonCards.edificeDeck.filterNot(ofSuit.tail.contains)),
      map = current.map.copy(sites = current.map.sites.updatedWith(
        buriedGiant)(_.map(site => site.copy(denizens = site.denizens ++
          ofSuit.tail.map(EdificeState(_, EdificeSide.Intact,
            Tokens.empty)))))))))
    val firstBuilt = accepted(rules.resolveWalker(
      atBuildChoice(table, first._1).state, p1,
      BuildEdifice.decisionId(first._1), DecisionAnswer.ChooseManyAnswer(
        Vector(DecisionOptionRef.Denizen(first._2)))))
    assertEquals(ChronicleWalk.parked(firstBuilt.state).map(_.decisionId),
      Some(BuildEdifice.decisionId(second._1)))
    // Answered alone, not walked to the end: Buried Giant holds more
    // edifices than a site can store, so the Chronicle could not be
    // recorded. The walk parks again in the World, after the Throne.
    val secondBuilt = accepted(rules.resolveWalker(firstBuilt.state, p1,
      BuildEdifice.decisionId(second._1), DecisionAnswer.ChooseManyAnswer(
        Vector(DecisionOptionRef.Denizen(second._2)))))
    val done = OathTransition(secondBuilt.state, secondBuilt.events)
    assertEquals(denizensAt(done, ancientCity).count(_.isInstanceOf[
      EdificeState]), 2)

  test("cards at sites the Empire does not rule are not offered"):
    val at = atBuildChoice(empireAtAncientCity
      .denizen(ThreateningRoarCard, at = BrokenPeaksSite), roarSuit)
    assert(!ChronicleWalk.parked(at.state)
      .exists(_.decisionId == BuildEdifice.decisionId(roarSuit)))

  test("building writes its line"):
    val table = empireAtAncientCity
      .denizen(ThreateningRoarCard, at = AncientCitySite)
    val edifice = offered(table, roarSuit)
    val done = build(atBuildChoice(table, roarSuit), roarSuit,
      Vector(DecisionOptionRef.Denizen(roar)))
    val id = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
    assert(NoteText.said(id, ChronicleThroneProcedure.noteKeys, done.events)
      .map(_.text).contains(s"${p1.value} built ${edifice.value} at " +
        s"${ancientCity.value}"), done.events)
