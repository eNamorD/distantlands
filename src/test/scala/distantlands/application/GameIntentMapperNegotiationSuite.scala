package distantlands.application

import distantlands.model._
import distantlands.protocol.{GameIntent => Intent, DecisionAnswerWire => Wire}

class GameIntentMapperNegotiationSuite extends munit.FunSuite:
  private val red = PlayerId("red")

  private def bound(answer: Wire) =
    GameIntentMapper.map(Intent.ResolveWalker("negotiation.deal", answer))

  test("an adviser in a transfer row maps to its world card"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, Vector(distantlands.protocol.WorldCard("vision", "v1"),
          distantlands.protocol.WorldCard("denizen", "d1")))),
      Vector.empty))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        Vector(NegotiationTransfer(PlayerId("blue"), 0, Vector.empty,
          Vector(VisionId("v1"), DenizenId("d1"))))))))))
    val unknown = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, Vector(distantlands.protocol.WorldCard("relic", "r1")))),
      Vector.empty))
    assertEquals(bound(unknown).left.map(_.path),
      Left("$.intent.terms.transfers.advisers.kind"))

  test("a site in a transfer row maps to its site id"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, Vector.empty, Vector("site:dunes"))), Vector.empty))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        Vector(NegotiationTransfer(PlayerId("blue"), 0, Vector.empty,
          Vector.empty, Vector(SiteId("site:dunes"))))))))))

  test("secrets in a transfer row map to the engine's row"):
    val wire = Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
      Vector(distantlands.protocol.NegotiationTransfer("blue", 0,
        Vector.empty, secrets = 2)), Vector.empty))
    assertEquals(bound(wire), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        Vector(NegotiationTransfer(PlayerId("blue"), 0, Vector.empty,
          secrets = 2))))))))

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
    // Trimming can make two promises one.
    assertEquals(bound(terms(Vector("Peace.", " Peace."))), Left(
      GameIntentMappingFailure("$.intent.terms.promises",
        "the same promise twice")))
    assertEquals(bound(terms(Vector.tabulate(6)(i => s"Promise $i")))
      .left.map(_.message), Left("more than 5 promises"))

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
