package distantlands.application

import distantlands.model._
import distantlands.protocol.{GameIntent => Intent, DecisionAnswerWire => Wire}

class GameIntentMapperNegotiationSuite extends munit.FunSuite:
  private val red = PlayerId("red")

  private def bound(answer: Wire) =
    GameIntentMapper.map(Intent.ResolveWalker("negotiation.deal", answer))

  test("a proposed terms wire answer maps to the engine's terms"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 2, Vector("r1"))),
      Vector(distantlands.protocol.NegotiationDisclosure("blue",
        distantlands.protocol.NegotiationInformation.HeldRelic("red", "r2")))))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(
      TreeDecision("negotiation.deal", DecisionAnswer.ProposeTerms(
        NegotiationTerms(Vector(NegotiationTransfer(PlayerId("blue"), 2,
          Vector(RelicId("r1")))), Vector(NegotiationDisclosure(PlayerId("blue"),
          NegotiationDisclosureRef.HeldRelic(red, RelicId("r2"))))))))))

  test("promises are trimmed, and a promise over a limit is refused"):
    def terms(promises: Vector[String], binding: Vector[String] = Vector.empty) =
      Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
        Vector.empty, Vector.empty, Vector.empty, promises, binding))
    assertEquals(bound(terms(Vector("  I will not attack you.  "),
      Vector(" Binding. "))), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        promises = Vector("I will not attack you."),
        bindingPromises = Vector("Binding.")))))))
    assertEquals(bound(terms(Vector("x" * 161))), Left(
      GameIntentMappingFailure("$.intent.terms.promises",
        "a promise over 160 characters")))
    assertEquals(bound(terms(Vector.empty, Vector("   "))), Left(
      GameIntentMappingFailure("$.intent.terms.bindingPromises",
        "a blank promise")))
