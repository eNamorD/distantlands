package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.NegotiationStartFacts
import distantlands.model._
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
