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
