package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Outcomes
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Score Lineage Points (CR p. 37, Chronicle ruling 13): p1 won and scores
  * 3, and may give 1 point to another seated player for 2 of theirs. */
class LineageScoringSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def atGifts(using munit.Location): OathTransition =
    ChronicleWalk.walkedTo(rules, accepted(rules.next(OathTransition(
      ChronicleWalk.throneHeldByChancellorP1.warbandsAt("Ancient City", p1, 1)
        .state, Vector.empty), TriggeredProcedureRef.ChronicleSun)),
      LineageScoring.giftsDecisionId)

  private def gifting(toP2: Int, toP3: Int)(using munit.Location)
      : OathTransition =
    val at = atGifts
    val next = accepted(rules.resolveWalker(at.state, p1,
      LineageScoring.giftsDecisionId, DecisionAnswer.DistributeAnswer(Vector(
        DistributeAmount(DecisionOptionRef.Player(p2), toP2),
        DistributeAmount(DecisionOptionRef.Player(p3), toP3)))))
    OathTransition(next.state, at.events ++ next.events)

  private def scoreOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.campaign.era.score(
      ready.game.current.players.find(_.player == player).get.lineage)

  test("the winner may give the other seated players one point for two"):
    val parked = ChronicleWalk.parked(atGifts.state).get
    assertEquals(parked.owner, p1)
    parked.query match
      case spread: DecisionQuery.Distribute =>
        assertEquals((spread.slots.map(slot => slot.ref -> slot.maximum),
          spread.minTotal, spread.maxTotal), (Vector(
            DecisionOptionRef.Player(p2) -> 1,
            DecisionOptionRef.Player(p3) -> 1), 0, 1))
      case other => fail(other.toString)
    val before = Outcomes.ready(atGifts.state)
    val after = Outcomes.ready(gifting(1, 0).state)
    assertEquals(Vector(p1, p2, p3).map(player =>
      scoreOf(after, player) - scoreOf(before, player)), Vector(1, 1, 0))

  test("keeping every point scores 3"):
    val before = Outcomes.ready(atGifts.state)
    val after = Outcomes.ready(gifting(0, 0).state)
    assertEquals(scoreOf(after, p1) - scoreOf(before, p1), 3)

  test("the lines name the points scored and given"):
    val id = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
    assertEquals(NoteText.said(id, ChronicleThroneProcedure.noteKeys,
      gifting(1, 0).events).map(_.text).filter(_.contains("lineage point")),
      Vector(s"${p1.value} scored 1 lineage point",
        s"${p2.value} scored 1 lineage point",
        s"${p1.value} gave 1 lineage point to ${p2.value}"))
