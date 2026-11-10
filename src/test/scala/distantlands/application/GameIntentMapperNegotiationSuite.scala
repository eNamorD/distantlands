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

  // The command wire's decoder refuses all of these, so no client reaches
  // them today; the mapper still answers a typed failure, never a throw,
  // for terms that skipped it.
  test("terms the model refuses are a typed failure, not an exception"):
    import distantlands.protocol.{NegotiationDisclosure => WireDisclosure,
      NegotiationInformation => Information, NegotiationTerms => WireTerms,
      NegotiationTransfer => WireTransfer}
    def refused(terms: WireTerms) =
      bound(Wire.ProposeTermsWire(terms)).left.map(_.path)
    val shown = WireDisclosure("blue", Information.HeldRelic("red", "r2"))
    Vector(
      WireTerms(Vector(WireTransfer("blue", 1, Vector.empty),
        WireTransfer("blue", 2, Vector.empty)), Vector.empty),
      WireTerms(Vector.empty, Vector(shown, shown)),
      WireTerms(Vector(WireTransfer("blue", -1, Vector.empty)), Vector.empty),
      WireTerms(Vector(WireTransfer("blue", 0, Vector("r1", "r1"))),
        Vector.empty),
      WireTerms(Vector.empty, Vector.empty, Vector("blue", "blue")))
      .foreach(terms => assertEquals(refused(terms), Left("$.intent.terms"),
        terms))
