package distantlands.application

import distantlands.gameplay.setup.{LaterGameFixture, LegacyDeal, LegacyKeep,
  SetupLegacyFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.MatchmakerCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Situation

/** Who sees a draw Matchmaker's holder keeps (spec ruling 13; R16): the
  * holder, and not the owner, while it waits; the kept legacy, its owner
  * alone once kept; the returned one, nobody. p3 holds Matchmaker. */
class MatchmakerProjectionSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val projector = new GameProjector(catalog)
  private val (l3, p1, p2, p3) = (LineageId("l3"), PlayerId("p1"),
    PlayerId("p2"), PlayerId("p3"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2")), l3,
    Role.Exile, MatchmakerCard.id -> true)
  private val orders = LaterGameFixture.orders(record, p1)
  private val drawn = LegacyDeal.of(record, orders.participants)
    .drawn(orders.legacyOrder, p2)

  private def parkedAt(decisionId: String) =
    Situation.start(Situation.rules(catalog).withAnswers {
      case park if park.decisionId == LegacyKeep.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Legacy(drawn.last))
    }).parkedAt(decisionId)(GameCommand.Begin(record, orders))

  test("before p2's keep is reached, nobody identifies p2's draw (R16)"):
    val at = parkedAt(LegacyKeep.decisionId(p1))
    drawn.foreach(legacy => Vector(Some(p3), Some(p2), Some(p1), None)
      .foreach(viewer => assert(
        !presentation.identifiesAt(at.ready, viewer, legacy),
        s"$viewer $legacy")))

  test("while p2's keep waits, only the holder identifies p2's draw"):
    val at = parkedAt(LegacyKeep.decisionId(p2))
    drawn.foreach { legacy =>
      assert(presentation.identifiesAt(at.ready, Some(p3), legacy), legacy)
      Vector(Some(p2), Some(p1), None).foreach(viewer =>
        assert(!presentation.identifiesAt(at.ready, viewer, legacy),
          s"$viewer $legacy"))
    }
    val loaded = LoadedGame(at.state, 1L)
    assert(projector.project("match", loaded, p3).walkerDecision
      .exists(_.query.exists(_.offeredOptions.size == 2)))
    assert(!projector.project("match", loaded, p2).walkerDecision
      .exists(_.query.nonEmpty))

  test("once kept, the owner alone identifies the kept legacy and nobody " +
      "the returned one"):
    val after = parkedAt(LegacyKeep.decisionId(p3)).ready
    assert(presentation.identifiesAt(after, Some(p2), drawn.last))
    Vector(Some(p3), Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(after, viewer, drawn.last),
        viewer))
    Vector(Some(p2), Some(p3), Some(p1), None).foreach(viewer =>
      assert(!presentation.identifiesAt(after, viewer, drawn.head), viewer))

  test("without Matchmaker the owner identifies their own draw"):
    val plain = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val plainOrders = LaterGameFixture.orders(plain, p1)
    val own = LegacyDeal.of(plain, plainOrders.participants)
      .drawn(plainOrders.legacyOrder, p2)
    val at = Situation.start(Situation.rules(catalog))
      .parkedAt(LegacyKeep.decisionId(p2))(GameCommand.Begin(plain,
        plainOrders)).ready
    own.foreach(legacy => assert(presentation.identifiesAt(at, Some(p2),
      legacy), legacy))
