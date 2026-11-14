package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{HighPriestCard,
  NegotiationAdvisers, NegotiationStartFacts}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.legacies.LegacyActionFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** A Negotiation's start facts as the walker stores them. */
class NegotiationStartFactsSuite extends munit.FunSuite:
  test("the facts read back as they were written"):
    Vector(NegotiationStartFacts(Vector(p2, p3), Vector(p1)),
        NegotiationStartFacts(Vector(p2), Vector.empty)).foreach(facts =>
      assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts)))

  test("facts with no marker, or with a ref that names no player, are " +
      "refused"):
    val refused = Vector(
      Vector[DecisionOptionRef](DecisionOptionRef.Player(p2)),
      Vector[DecisionOptionRef](DecisionOptionRef.Site(SiteId("s1")),
        DecisionOptionRef.Button(NegotiationStartFacts.Marker)),
      Vector.empty[DecisionOptionRef])
    refused.foreach(refs => assert(NegotiationStartFacts.read(refs).left
      .toOption.exists(_.isInstanceOf[OathViolation.InvalidEventOrder]),
      refs))

  test("a deal takes binding promises when a participant may use The " +
      "Tribunal"):
    val facts = NegotiationStartFacts(Vector(p2, p3), Vector(p3))
    assert(facts.bindingPromises(Vector(p1, p3)))
    assert(!facts.bindingPromises(Vector(p1, p2)))

  test("the adviser users round trip, and open the rules"):
    val facts = NegotiationStartFacts(Vector(p2, p3), Vector.empty,
      Vector(p3), Vector(p2))
    assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts))
    assertEquals(facts.rules(Vector(p1, p2)).advisersOpen, true)
    assertEquals(facts.rules(Vector(p1, p3)).adviserTraders, Vector(p3))
    assertEquals(facts.rules(Vector(p1, p3)).advisersOpen, false)
    assert(NegotiationStartFacts.read(Vector(DecisionOptionRef.Player(p2),
      DecisionOptionRef.Button(NegotiationStartFacts.Marker))).isLeft)

  test("Deed Writer's users round trip, and open site terms"):
    val facts = NegotiationStartFacts(Vector(p2, p3), Vector.empty,
      Vector.empty, Vector.empty, Vector(p3))
    assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts))
    assertEquals(facts.rules(Vector(p1, p3)).siteTraders, Vector(p3))
    assertEquals(facts.rules(Vector(p1, p3)).siteRecipients(p1,
      Vector(p1, p3)), Vector(p3))
    assertEquals(facts.rules(Vector(p1, p2)).siteTraders,
      Vector.empty[PlayerId])

  test("High Priest's users round trip, and open one-way secret terms " +
      "(Legacy in-play powers ruling 19)"):
    val facts = NegotiationStartFacts(Vector(p2), Vector.empty,
      highPriestUsers = Vector(p1))
    assertEquals(NegotiationStartFacts.read(facts.refs), Right(facts))
    assertEquals(facts.rules(Vector(p1, p2)).secretGivers, Vector(p1))
    assertEquals(facts.rules(Vector(p2, p3)).secretGivers,
      Vector.empty[PlayerId])

  test("facts saved before High Priest's list decode with it empty (R9)"):
    val saved = Vector[DecisionOptionRef](DecisionOptionRef.Player(p2),
      DecisionOptionRef.Button(NegotiationStartFacts.Marker),
      DecisionOptionRef.Button(NegotiationStartFacts.ArbiterMarker),
      DecisionOptionRef.Button(NegotiationStartFacts.FestivalMarker),
      DecisionOptionRef.Button(NegotiationStartFacts.DeedWriterMarker))
    assertEquals(NegotiationStartFacts.read(saved),
      Right(NegotiationStartFacts(Vector(p2), Vector.empty)))

  test("only a seated holder of an active High Priest among the players " +
      "is a user"):
    def users(table: Table, among: Vector[PlayerId] = Vector(p1, p2, p3)) =
      NegotiationAdvisers.highPriestUsers(catalog, table.ready, among,
        Vector.empty)
    assertEquals(users(Table.start.legacy(p1, HighPriestCard)), Vector(p1))
    assertEquals(users(Table.start.legacy(p1, HighPriestCard,
      active = false)), Vector.empty[PlayerId])
    assertEquals(users(Table.start.legacy(p1, HighPriestCard),
      among = Vector(p2, p3)), Vector.empty[PlayerId])

  test("a boxed lineage's High Priest gives no user (ruling 1)"):
    val boxed = LegacyActionFixture.boxed(
      Table.start.legacy(p1, HighPriestCard).ready, p1)
    assertEquals(NegotiationAdvisers.highPriestUsers(catalog, boxed,
      Vector(p1, p2, p3), Vector.empty), Vector.empty[PlayerId])

  test("a private start's candidates are every other player, wherever " +
      "their pawns stand"):
    val ready = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).pawn(p3, BuriedGiantSite).ready
    assertEquals(NegotiationStartFacts.at(catalog, ready, p1, Vector.empty,
      privately = true).candidates, Vector(p2, p3))
    assertEquals(NegotiationStartFacts.at(catalog, ready, p1, Vector.empty)
      .candidates, Vector.empty)
